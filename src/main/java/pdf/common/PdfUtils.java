package pdf.common;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import pdf.anno.Font;
import pdf.anno.*;
import pdf.config.style.FontConfig;
import pdf.enums.AlignEnum;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 11:34
 * @date 2026-05-12
 */
@Api(tags = "PDF工具类")
@Slf4j
public class PdfUtils {
    private Map<String, List<String>> strs = new HashMap<>();
    // private final float itemMargin = 4.5f;
    private float minY = Float.MAX_VALUE;
    private FontConfig fontConfig;
    private float pageWidth;
    public final float ptConvert = 2.83465f;
    private float pageHeight;

    /**
     * @param model: 需要填充的模版类
     * @return 最终结果的PDDocument对象
     * @throws Exception
     */
    public PDDocument generatePdfDocument(Object model) throws Exception {
        return modifyPdfDocument(model, "");
    }

    /**
     * 在已有的模板上填充内容，建议提前调用测量方法：getTotalLength测量页面高度再创建模板，避免高度不一致导致排版错乱。
     *
     * @param model:                      需要填充的模版类
     * @param modelPath：前置文件的路径，在已有pdf上填充
     * @return 最终结果的PDDocument对象
     * @throws Exception
     */
    public PDDocument modifyPdfDocument(Object model, String modelPath) throws Exception {

        Class<?> modelClass = model.getClass();
        ModelSize modelSize = modelClass.getAnnotation(ModelSize.class);
        PDDocument document = loadPdfTemplate(modelPath);
        PDPage page;
        float pageHeightPt, pageWidthPt;
        //  创建内容流
        PDPageContentStream contentStream;
        //字体初始化
        if (document == null) {
            //创建页面
            document = new PDDocument();
            fontConfig = new FontConfig(document);
            //计算页面高度
            pageHeight = getTotalLength(model);
            pageWidth = modelSize.width();
            //毫米转pt点
            pageWidthPt = pageWidth * ptConvert;
            pageHeightPt = pageHeight * ptConvert;
            document = new PDDocument();
            //创建页面并新增
            page = new PDPage(new PDRectangle(pageWidthPt, pageHeightPt));
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
        } else {
            //已存在页面需要初始化游标
            initMoveY(model);
            page = document.getPage(0);
            contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
            pageHeightPt = page.getMediaBox().getHeight();
            pageWidth = page.getMediaBox().getWidth() / ptConvert;
        }
        //先绘制文字
        Field[] listField = model.getClass().getDeclaredFields();
        //初始化游标为可变元素最小值
        float moveY = minY;
        for (Field field : listField) {
            field.setAccessible(true);
            float xMM, yMM;
            if (field.isAnnotationPresent(PdfList.class)) {
                //判断是否有注解,来确定列表之间、列表内元素之间是否需要上下边距
                PdfList pdfList = field.getAnnotation(PdfList.class);
                float listMargin = pdfList.listMargin();
                List<?> list = (List<?>) field.get(model);
                for (Object o : list) {
                    Field[] declaredFields = o.getClass().getDeclaredFields();
                    for (Field declaredField : declaredFields) {
                        declaredField.setAccessible(true);
                        if (declaredField.isAnnotationPresent(Position.class) && declaredField.isAnnotationPresent(Font.class)
                                && StringUtils.isNotBlank((String) declaredField.get(o))) {
                            Font font = declaredField.getAnnotation(Font.class);
                            Position position = declaredField.getAnnotation(Position.class);
                            if (position.alignType().equals(AlignEnum.SELF)) {
                                continue;
                            }
                            String value = (String) declaredField.get(o);
                            String key = position.title() + value + "|" + font.fontType() + "|" + font.fontSize();
                            List<String> strings = strs.get(key);
                            //获取字体
                            PDFont pdFont = fontConfig.getPDFont(font.fontType());
                            for (String string : strings) {
                                //每个字符串都要根据对齐方式计算实际左边距
                                xMM = getXMMbyPosition(position, font, pdFont, string);
                                if (!position.alignType().equals(AlignEnum.VERTICAL)) {
                                    //游标位置默认文字左上角，而绘制起点是左下角，需要偏移。
                                    yMM = moveY + getFontMM(font.fontSize());
                                    //游标偏移，加上底边距和字体高度
                                    moveY += position.stringMargin() + getFontMM(font.fontSize());
                                } else {
                                    //垂直居中，重新计算y坐标，游标无需移动
                                    yMM = (pageHeight - getFontMM(font.fontSize())) / 2;
                                }
                                drawText(contentStream, fontConfig, string, font, xMM * ptConvert, yMM * ptConvert);
                            }
                        }
                    }
                    //不同列表之间的元素，需要复用注解上的底边距进行偏移
                    moveY += listMargin;
                }
            }
            //处理掉模版固定位置内容
            else if (field.isAnnotationPresent(Position.class)) {
                Position position = field.getAnnotation(Position.class);
                if (position.alignType().equals(AlignEnum.SELF)) {
                    continue;
                }
                if (field.isAnnotationPresent(Font.class)) {
                    Font font = field.getAnnotation(Font.class);
                    String text = position.title() + field.get(model);
                    PDFont pdFont = fontConfig.getPDFont(font.fontType());
                    xMM = getXMMbyPosition(position, font, pdFont, text);
                    yMM = getYMMbyPosition(position, font, pageHeight);
                    drawText(contentStream, fontConfig, text, font, xMM * ptConvert, yMM * ptConvert);
                }
                if (field.isAnnotationPresent(ImageStyle.class)) {
                    ImageStyle imageStyle = field.getAnnotation(ImageStyle.class);
                    String path = (String) field.get(model);
                    drawImage(document, contentStream, path, imageStyle.width() * ptConvert, imageStyle.height() * ptConvert,
                            position.positionX() * ptConvert, position.positionY() * ptConvert, pageHeightPt);
                }
            }
        }
        System.out.println("高度为：" + pageHeight + "宽度为" + pageWidth);
        contentStream.close();
        return document;
    }

    public float getTotalLength(Object model) throws IllegalAccessException, IOException {
        if (model.getClass().isAnnotationPresent(ModelSize.class)) {
            ModelSize modelSize = model.getClass().getAnnotation(ModelSize.class);
            float sum = 0;
            //先处理外部模版
            if (modelSize == null || modelSize.height() == 0) {
                log.error("请使用正确的模版！");
                System.exit(1);
            }
            //先默认不可变部分white占据模版所有高度，后续直接做减法
            float height = modelSize.height(), white = height;
            //先求出固定部分高度
            Field[] listField = model.getClass().getDeclaredFields();
            for (Field field : listField) {
                if (field.isAnnotationPresent(PdfList.class)) {
                    field.setAccessible(true);
                    PdfList pdfList =  field.getAnnotation(PdfList.class);
                    float listMargin = pdfList.listMargin();
                    Type genericType = field.getGenericType();
                    Object listObj = field.get(model);
                    if (listObj instanceof List) {
                        List<?> list = (List<?>) field.get(model);
                        //直接使用该列表获取实际占据的高度
                        sum += getListLength(list, listMargin);
                        ParameterizedType pt = (ParameterizedType) genericType;
                        Type[] actualTypes = pt.getActualTypeArguments();
                        // 取第一个泛型参数，即 List<T> 中的 T
                        Class<?> elementClass = (Class<?>) actualTypes[0];
                        //这里计算模版中的可变部分高度
                        white -= getItemLength(elementClass, listMargin);
                    }
                }
            }
            sum += white;
            return sum;
        } else {
            return 0;
        }
    }

    //获取实际的列表高度,同时缓存字符串换行结果，这里不考虑递归情况
    public float getListLength(List<?> list, float listMargin) throws IllegalAccessException, IOException {
        float len = 0;
        for (Object o : list) {
            Field[] fields = o.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Position.class) && field.isAnnotationPresent(Font.class)
                        && StringUtils.isNotBlank((String) field.get(o))) {
                    Position position = field.getAnnotation(Position.class);
                    Font font = field.getAnnotation(Font.class);
                    String str = position.title() + field.get(o);
                    //将字符串、字体、字号拼接作为key。便于缓存换行结果
                    String key = str + "|" + font.fontType() + "|" + font.fontSize();
                    int fontSize = font.fontSize();
                    //换行，换行的宽度阈值设置为180mm
                    splitString(str, key, font, 180);
                    //累加时需要用换行后字符串数量 * （字符高度+底边距）
                    len += strs.get(key).size() * (getFontMM(fontSize) + position.stringMargin());
                }
            }
            //如果所有元素都为空，就不加底边距
            if (len != 0) {
                len += listMargin;
            }
        }
        return len;
    }

    /**
     * 获取可变行元素的默认初始高度，也就是模版中的列表所在类中，所有需要填充的文本初始的行总高度
     */
    public float getItemLength(Class<?> cls, float listMargin) {
        //yMin指左上边界，yMax指左下边界（包含底边距）
        float yMin = Float.MAX_VALUE, yMax = 0, itemMargin;
        //反射逻辑
        Field[] declaredFields = cls.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Position.class) && field.isAnnotationPresent(Font.class)) {
                field.setAccessible(true);
                Position position = field.getAnnotation(Position.class);
                Font font = field.getAnnotation(Font.class);
                int fontSize = font.fontSize();
                float fontHeight = getFontMM(fontSize);
                itemMargin = position.stringMargin();
                minY = Math.min(minY, position.positionY());
                yMin = Math.min(yMin, position.positionY());
                //最低点也是y最大时，需要算上文字高度和底边距
                yMax = Math.max(yMax, position.positionY() + fontHeight + itemMargin);
            }
        }
        if (yMin != Float.MAX_VALUE) {
            yMax += listMargin;
        }
        //0表示如果某个列表漏加注解就不计算，跳过。
        return yMin == Float.MAX_VALUE ? 0 : yMax - yMin;
    }

    //文本换行并保存
    public List<String> splitString(String text, String key, Font font, float maxWidthMM) throws IOException {
        // 2. 命中缓存则直接返回
        if (strs.containsKey(key)) {
            return strs.get(key);
        }
        PDFont pdFont = fontConfig.getPDFont(font.fontType());
        List<String> lines = new ArrayList<>();
        int len = text.length();
        int start = 0;
        while (start < len) {
            int end = start;
            float currentWidth = 0f;
            // 累加每个字符的宽度
            while (end < len) {
                char c = text.charAt(end);
                float charWidth = pdFont.getStringWidth(String.valueOf(c)) / 1000f * font.fontSize();
                // 如果加上当前字符超出最大宽度，换行
                if (currentWidth + charWidth > getFontPt(maxWidthMM)) {
                    break;
                }
                currentWidth += charWidth;
                end++;
            }
            // 避免死循环：如果一个字符都放不下，强制截取一个字符
            if (start == end) {
                end = start + 1;
            }
            lines.add(text.substring(start, end));
            start = end;
        }
        // 3. 存入缓存，Key = text + "|" + fontName + "|" + fontSize
        strs.put(key, lines);
        return lines;
    }

    public float getFontMM(int fontSize) {
        return fontSize / ptConvert;
    }

    public float getFontPt(float fontMM) {
        return fontMM * ptConvert;
    }

    //字符串颜色转Color对象
    private Color parseColor(String colorHex) {
        if (colorHex == null || colorHex.isEmpty() || !colorHex.startsWith("#")) {
            return Color.BLACK;
        }
        try {
            int r = Integer.parseInt(colorHex.substring(1, 3), 16);
            int g = Integer.parseInt(colorHex.substring(3, 5), 16);
            int b = Integer.parseInt(colorHex.substring(5, 7), 16);
            return new Color(r, g, b);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }

    //文本绘制
    public void drawText(PDPageContentStream contentStream, FontConfig fontConfig, String text, Font font, float xPt, float yPt) throws IOException {
        int fontSize = font.fontSize();
        PDFont pdFont = fontConfig.getPDFont(font.fontType());
        Color color = parseColor(font.color());
        contentStream.beginText();
        contentStream.setFont(pdFont, fontSize);
        contentStream.setNonStrokingColor(color);  // 设置文字颜色
        contentStream.newLineAtOffset(xPt, pageHeight * ptConvert - yPt);
        contentStream.showText(text);
        contentStream.endText();
    }

    //图片绘制
    public void drawImage(PDDocument document, PDPageContentStream contentStream, String imagePath, float widthPt, float heightPt,
                          float xPt, float yPt, float pageHeightPt) throws IOException {
        // 1. 从路径加载图片，支持根路径和包路径
//        PDImageXObject pdImage = PDImageXObject.createFromFile(imagePath, document);
        // 处理开头的斜杠（ClassLoader 不需要斜杠）
        String normalizedPath = imagePath;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        InputStream imageStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(normalizedPath);
        if (imageStream == null) {
            throw new IOException("图片资源不存在: " + imagePath);
        }
        byte[] imageBytes = IOUtils.toByteArray(imageStream);
        imageStream.close();
        // 2. 从 InputStream 创建 PDImageXObject
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, imagePath);

        // 3. 绘制图片（坐标原点在左下角）
        contentStream.drawImage(pdImage, xPt, pageHeightPt - yPt - heightPt, widthPt, heightPt);

    }

    private float getXMMbyPosition(Position position, Font font, PDFont pdFont, String text) throws IOException {
        float xMM;
        if (!position.alignType().equals(AlignEnum.HORIZONTAL)) {
            xMM = position.positionX();
        } else {
            xMM = (pageWidth - pdFont.getStringWidth(text) / 1000 * font.fontSize() / ptConvert) / 2;
        }
        return xMM;
    }

    private float getYMMbyPosition(Position position, Font font, float pageHeight) {
        float yMM;
        if (!position.alignType().equals(AlignEnum.VERTICAL)) {
            yMM = position.positionY();
        } else {
            yMM = (pageHeight - getFontMM(font.fontSize())) / 2;
        }
        return yMM;
    }

    public PDDocument loadPdfTemplate(String path) throws IOException {
        PDDocument document;
        InputStream is = null;
        // 1. 优先作为 classpath 资源加载（支持相对路径，resources 下的文件）
        if (StringUtils.isNotBlank(path)) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (is == null && path.startsWith("/")) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path.substring(1));
            }
        }
        // 2. 降级：作为文件系统路径加载（支持绝对路径和相对路径）
        if (is == null && StringUtils.isNotBlank(path)) {
            File file = new File(path);
            if (file.exists()) {
                return PDDocument.load(file);
            }
        }

        // 3. 如果都打不开
        if (is == null) {
            return null;
        }

        // 4. 从 InputStream 加载
        document = PDDocument.load(is);
        is.close();
        return document;
    }

    private void initMoveY(Object model) throws IllegalAccessException {
        Field[] declaredFields = model.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.isAnnotationPresent(PdfList.class)) {
                Type genericType = declaredField.getGenericType();
                declaredField.setAccessible(true);
                Object listObj = declaredField.get(model);
                if (listObj instanceof List) {
                    ParameterizedType pt = (ParameterizedType) genericType;
                    Type[] actualTypeArguments = pt.getActualTypeArguments();
                    Class<?> cls = (Class<?>) actualTypeArguments[0];
                    for (Field field : cls.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Position.class)) {
                            field.setAccessible(true);
                            Position position = field.getAnnotation(Position.class);
                            minY = Math.min(minY, position.positionY());
                        }
                    }
                }
            }
        }
    }
}

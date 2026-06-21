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
    private final Map<String, List<String>> strs = new HashMap<>();
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
     * @param model:需要填充的模版类
     * @param modelPath：前置文件的路径，在已有pdf上填充
     * @return 最终结果的PDDocument对象
     * @throws Exception
     */
    public PDDocument modifyPdfDocument(Object model, String modelPath) throws Exception {
        //获取模版宽高，宽默认210mm（A4纸宽）
        if (!model.getClass().isAnnotationPresent(ModelSize.class)) {
            log.error("错误！关键注解未配置：ModelSize");
            return null;
        }
        ModelSize modelSize = model.getClass().getAnnotation(ModelSize.class);
        //加载已有的pdf（如果有），路径为空就新建pdf
        PDDocument document = loadPdfTemplate(modelPath);
        PDPage page;
        //定义的页面宽高对应的pt点数
        float pageHeightPt, pageWidthPt;
        //创建内容流
        PDPageContentStream contentStream;
        if (document == null) {
            //创建页面
            document = new PDDocument();
            //页面宽度，由模版定义
            pageWidth = modelSize.width();
            //计算页面高度
            pageHeight = getTotalLength(model, modelSize);
            //毫米转pt点
            pageWidthPt = pageWidth * ptConvert;
            pageHeightPt = pageHeight * ptConvert;
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
        //字体初始化
        fontConfig = new FontConfig(document);
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
                    Map<Float, Float> map = new HashMap<>();
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
                            float strTotalHeight = strings.size() * ((getFontMM(font.fontSize()) + position.stringMargin()));
                            yMM = moveY + getFontMM(font.fontSize());
                            if (!map.containsKey(position.positionY())) {
                                map.put(position.positionY(), strTotalHeight);
                                moveY += strTotalHeight;
                            } else {
                                float maxTotalHeight = map.get(position.positionY());
                                yMM = moveY - maxTotalHeight + getFontMM(font.fontSize());
                                if (strTotalHeight > maxTotalHeight) {
                                    map.put(position.positionY(), strTotalHeight);
                                    moveY += strTotalHeight - maxTotalHeight;
                                }
                            }
                            for (String string : strings) {
                                //每个字符串都要根据对齐方式计算实际左边距
                                xMM = getXMMbyPosition(position, font, pdFont, string);
                                drawText(contentStream, fontConfig, string, font, xMM * ptConvert, yMM * ptConvert);
                                //游标位置默认文字左上角，而绘制起点是左下角，需要偏移。
                                yMM += getFontMM(font.fontSize()) + position.stringMargin();
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
                    yMM = position.positionY();
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

    /**
     * 使用模版固定部分高度+可变列表实际高度计算页面高度
     *
     * @param model     模版对象
     * @param modelSize 获取的模版尺寸注解
     * @return 最终高度，单位mm
     * @throws IllegalAccessException
     * @throws IOException
     */
    public float getTotalLength(Object model, ModelSize modelSize) throws IllegalAccessException, IOException {
        float sum = 0;
        //先处理外部模版
        if (modelSize.height() <= 0 || modelSize.width() <= 0) {
            throw new IllegalArgumentException("模版宽高不合法！");
        }
        //先默认固定部分white占据模版所有高度，后续直接做减法
        float height = modelSize.height(), white = height;
        Field[] listField = model.getClass().getDeclaredFields();
        //遍历模版的每一个字段，求出固定部分高度
        for (Field field : listField) {
            if (!field.isAnnotationPresent(PdfList.class)) {
                continue;
            }
            //找出其中需要绘制的可变列表
            field.setAccessible(true);
            PdfList pdfList = field.getAnnotation(PdfList.class);
            //获取不同列表之间的间距
            float listMargin = pdfList.listMargin();
            Object listObj = field.get(model);
            if (listObj instanceof List) {
                List<?> list = (List<?>) listObj;
                if (list.size() == 0) {
                    continue;
                }
                //直接使用该列表获取实际占据的高度
                sum += getListLength(list, listMargin);
                //这里计算模版中的可变部分高度
                white -= getItemLength(list.get(0), listMargin);
            }
        }
        sum += white;
        return sum;
    }

    //获取实际的列表高度,同时缓存字符串换行结果，这里不考虑递归情况
    public float getListLength(List<?> list, float listMargin) throws IllegalAccessException, IOException {
        float len = 0;
        //遍历列表中的所有元素，找最大最小值，相减。
        for (Object o : list) {
            Field[] fields = o.getClass().getDeclaredFields();
            Map<Float, Float> map = new HashMap<>();
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
                    splitString(str, key, font, font.maxWidth());
                    //换行后字符串总高
                    float stringHeight = strs.get(key).size() * (getFontMM(fontSize) + position.stringMargin());
                    //累加时需要用换行后字符串数量 * （字符高度+底边距）
                    if (map.containsKey(position.positionY())) {
                        if (map.get(position.positionY()) < stringHeight) {
                            map.put(position.positionY(), stringHeight);
                        }
                    } else {
                        map.put(position.positionY(), stringHeight);
                    }
                }
            }
            for (float height : map.values()) {
                len += height;
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
    public float getItemLength(Object obj, float listMargin) {
        //yMin指左上边界，yMax指左下边界（包含底边距）
        float yMin = Float.MAX_VALUE, yMax = 0, itemMargin;
        //反射逻辑
        Field[] declaredFields = obj.getClass().getDeclaredFields();
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

    public PDDocument loadPdfTemplate(String path) throws IOException {
        PDDocument document;
        InputStream is;
        if (StringUtils.isBlank(path)) {
            return null;
        }
        // 1. 优先作为 classpath 资源加载（支持相对路径，resources 下的文件）
        is = getClass().getClassLoader().getResourceAsStream(path);
        // 2. 降级，作为文件系统路径加载
        if (is == null) {
            File file = new File(path);
            if (file.exists()) {
                return PDDocument.load(file);
            }
            log.info("找不到文件：文件路径输入错误或不存在！");
            return null;
        }
        // 3. 从 InputStream 加载
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

package pdf.common;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pdf.anno.Font;
import pdf.anno.*;
import pdf.config.style.FontConfig;
import pdf.enums.AlignEnum;
import pdf.enums.FontTypeEnum;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
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
@Component
public class PdfUtils {
    private final Map<String, List<String>> strs = new HashMap<>();
    // private final float itemMargin = 4.5f;
    private float minY = Float.MAX_VALUE;

    @Autowired
    Map<FontTypeEnum, TrueTypeFont> trueTypeFontMap;

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
            //计算高度前，依赖document对象初始化字体
            fontConfig = new FontConfig(document,trueTypeFontMap);
            //计算页面高度
            pageHeight = getTotalLength(model, modelSize);
            pageHeight = 180;
            //毫米转pt点
            pageWidthPt = pageWidth * ptConvert;
            pageHeightPt = pageHeight * ptConvert;
            //创建页面并新增
            page = new PDPage(new PDRectangle(pageWidthPt, pageHeightPt));
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
        } else {
            //初始化字体
            fontConfig = new FontConfig(document,trueTypeFontMap);
            //已存在页面需要初始化游标
            initMoveY(model);
            page = document.getPage(0);
            contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
            pageHeight = page.getMediaBox().getHeight() / ptConvert;
            pageWidth = page.getMediaBox().getWidth() / ptConvert;
            pageHeightPt = pageHeight * ptConvert;
        }

        //初始化游标为可变元素最小值
        float moveY = minY;
        for (Field field : model.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            float xMM, yMM;
            if (field.isAnnotationPresent(PdfList.class)) {
                //判断是否有注解,来确定列表之间、列表内元素之间是否需要上下边距
                PdfList pdfList = field.getAnnotation(PdfList.class);
                float listMargin = pdfList.listMargin();
                List<?> list = getListByField(model, field);
                if (list == null || list.size() == 0) {
                    continue;
                }
                for (Object o : list) {
                    Field[] declaredFields = o.getClass().getDeclaredFields();
                    Map<Float, Float> map = new HashMap<>();
                    for (Field declaredField : declaredFields) {
                        declaredField.setAccessible(true);
                        if (isNotDrawString(o, declaredField)) {
                            continue;
                        }
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
                        float strTotalHeight = strings.size() * ((getMMByPt(font.fontSize()) + position.stringMargin()));
                        yMM = moveY + getMMByPt(font.fontSize());
                        if (!map.containsKey(position.positionY())) {
                            map.put(position.positionY(), strTotalHeight);
                            moveY += strTotalHeight;
                        } else {
                            float maxTotalHeight = map.get(position.positionY());
                            yMM = moveY - maxTotalHeight + getMMByPt(font.fontSize());
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
                            yMM += getMMByPt(font.fontSize()) + position.stringMargin();
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
                    if (StringUtils.isEmpty(path)) {
                        continue;
                    }
                    drawImage(document, contentStream, path, imageStyle.width() * ptConvert, imageStyle.height() * ptConvert,
                            position.positionX() * ptConvert, position.positionY() * ptConvert, pageHeightPt);
                }
            }
        }
        //System.out.println("高度为：" + pageHeight + "宽度为" + pageWidth);
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
        if (modelSize.height() <= 0 || modelSize.width() <= 0) {
            throw new IllegalArgumentException("模版宽高不合法！");
        }
        //先默认固定部分white占据模版所有高度，后续直接做减法
        float height = modelSize.height(), white = height;
        //遍历模版的每一个字段，求出固定部分高度
        for (Field field : model.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            List<?> list = getListByField(model, field);
            if (list == null) continue;
            PdfList pdfList = field.getAnnotation(PdfList.class);
            //直接使用该列表和注解获取实际占据的高度
            sum += getListLength(list, pdfList.listMargin());
            //这里使用坐标差值法计算模版中的写死的列表高度
            white -= getItemLength(list.get(0), pdfList.listMargin());
        }
        sum += white;
        return sum;
    }

    /**
     * 通过field对象获取所在的list对象
     *
     * @param model 模版对象
     * @param field 字段
     * @return List对象
     * @throws IllegalAccessException
     */
    private List<?> getListByField(Object model, Field field) throws IllegalAccessException {
        Object listObj = field.get(model);
        //分别判断是否配置注解、是否在List配置了注解、List长度是否合法
        if (!field.isAnnotationPresent(PdfList.class) || !(listObj instanceof List) || ((List<?>) listObj).size() == 0) {
            return null;
        }
        return (List<?>) listObj;
    }

    /**
     * 获取实际的列表高度,同时缓存字符串换行结果，这里不考虑递归情况
     *
     * @param list       列表本身
     * @param listMargin 列表之间的边距
     * @return 实际列表总高度
     * @throws IllegalAccessException
     * @throws IOException
     */

    public float getListLength(List<?> list, float listMargin) throws IllegalAccessException, IOException {
        float len = 0;
        //遍历列表中的所有元素，累加求出总高度。
        for (Object o : list) {
            //这个map存储每个y坐标下的文本占据的高度，用来累计高度、处理同一高度多个元素
            Map<Float, Float> map = new HashMap<>();
            for (Field field : o.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (isNotDrawString(o, field)) {
                    continue;
                }
                Position position = field.getAnnotation(Position.class);
                Font font = field.getAnnotation(Font.class);
                String str = position.title() + field.get(o);
                //将字符串、字体、字号拼接作为key。便于缓存换行结果
                String key = str + "|" + font.fontType() + "|" + font.fontSize();
                //换行，换行的宽度阈值默认为180mm
                List<String> strings = splitString(str, key, font);
                //换行后字符串总高,累加时需要用换行后字符串数量 * （字符高度+底边距）
                float stringHeight = strings.size() * (getMMByPt(font.fontSize()) + position.stringMargin());
                //如果两行文字在模版里是同一行，计算高度时只需累加最大的实际高度
                if (map.containsKey(position.positionY())) {
                    if (map.get(position.positionY()) < stringHeight) {
                        map.put(position.positionY(), stringHeight);
                    }
                } else {
                    map.put(position.positionY(), stringHeight);
                }
            }
            for (float height : map.values()) {
                len += height;
            }
            //如果所有元素不为空，就加入底边距
            if (len != 0) {
                len += listMargin;
            }
        }
        return len;
    }


    /**
     * 判断当前字符串是否需要绘制，如果注解缺失或者字符串为空就不绘制
     *
     * @param o     文本所在对象
     * @param field 反射获取到的字段
     * @return 判断结果，为true就不绘制
     * @throws IllegalAccessException 反射异常
     */
    private boolean isNotDrawString(Object o, Field field) throws IllegalAccessException {
        return !field.isAnnotationPresent(Position.class) || !field.isAnnotationPresent(Font.class)
                || !StringUtils.isNotEmpty((String) field.get(o));
    }

    /**
     * 坐标差值法，获取可变行元素的默认初始高度，也就是模版中的，列表所在类中，所有需要填充的文本初始的行总高度
     *
     * @param obj        列表对应的对象
     * @param listMargin 列表之间的边距
     * @return 计算出的高度
     */
    public float getItemLength(Object obj, float listMargin) {
        //yMin指上边界，yMax指下边界。都是文字最顶部和页面顶部距离
        float yMin = Float.MAX_VALUE, yMax = 0;
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(Position.class) || !field.isAnnotationPresent(Font.class)) {
                continue;
            }
            Position position = field.getAnnotation(Position.class);
            Font font = field.getAnnotation(Font.class);
            float fontHeightMM = getMMByPt(font.fontSize());
            //minY记录全局最小值
            minY = Math.min(minY, position.positionY());
            yMin = Math.min(yMin, position.positionY());
            //最低点也是y最大时，需要加上文字高度和底边距
            yMax = Math.max(yMax, position.positionY() + fontHeightMM + position.stringMargin());
        }
        //0表示如果某个列表漏加注解，就不计算并跳过。
        return yMin == Float.MAX_VALUE ? 0 : yMax - yMin + listMargin;
    }

    /**
     * 文本换行处理，采用滑动窗口换行算法，同时会以原串+字体+字号缓存文本换行结果
     *
     * @param text 需要换行的原字符串
     * @param key  存入Map缓存的key值，用原串+字体+字号以‘|‘分割拼接而成
     * @param font 文本样式，包含字体、字号
     * @return 换行结果，纯字符串文本
     * @throws IOException 获取字符宽度api可能抛出
     */
    public List<String> splitString(String text, String key, Font font) throws IOException {
        // 命中缓存则直接返回
        if (strs.containsKey(key)) {
            return strs.get(key);
        }
        //获取字体对象，用于计算宽度
        PDFont pdFont = fontConfig.getPDFont(font.fontType());
        List<String> lines = new ArrayList<>();
        //滑动窗口换行
        int len = text.length(), start = 0;
        while (start < len) {
            int end = start;
            //窗口内字符累计宽度
            float currentWidth = 0f;
            // 累加每个字符的宽度
            while (end < len) {
                char c = text.charAt(end);
                float charWidth = pdFont.getStringWidth(String.valueOf(c)) / 1000f * font.fontSize();
                // 如果加上当前字符超出最大宽度，换行
                currentWidth += charWidth;
                if (currentWidth > getPtByMM(font.maxWidth())) {
                    break;
                }
                end++;
            }
            //一个字符都放不下时，强制截取一个，并避免死循环
            if (start == end) {
                end = end + 1;
            }
            lines.add(text.substring(start, end));
            start = end;
        }
        strs.put(key, lines);
        return lines;
    }

    //pt点转毫米(mm)
    public float getMMByPt(int fontSize) {
        return fontSize / ptConvert;
    }

    //毫米(mm)转pt点
    public float getPtByMM(float fontMM) {
        return fontMM * ptConvert;
    }

    /**
     * 如果是传入的页面，需要初始化游标，确定绘制的起点。记录所有列表第一个元素的最小y下标即可
     *
     * @param model 模版对象
     * @throws IllegalAccessException 反射异常
     */
    private void initMoveY(Object model) throws IllegalAccessException {
        Field[] declaredFields = model.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
            List<?> list = getListByField(model, declaredField);
            if (list == null) {
                continue;
            }
            Object firstObj = list.get(0);
            for (Field field : firstObj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (isNotDrawString(firstObj, field)) {
                    continue;
                }
                Position position = field.getAnnotation(Position.class);
                minY = Math.min(minY, position.positionY());
            }
        }
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


}

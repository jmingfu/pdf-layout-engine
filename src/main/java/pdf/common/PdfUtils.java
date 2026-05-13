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
    private static Map<String, List<String>> strs = new HashMap<>();
    private static final float itemMargin = 4.5f;
    private static float minY = Float.MAX_VALUE;
    public static FontConfig fontConfig;
    private static float pageWidth;
    public static final float ptConvert = 2.83465f;
    public static float pageHeight;

    public static String generatePdf(Object model, String modelPath) throws IOException, IllegalAccessException {

        Class<?> modelClass = model.getClass();
        ModelSize modelSize = modelClass.getAnnotation(ModelSize.class);
        PDDocument document = loadPdfTemplate(modelPath);
        PDPage page;
        float pageHeightPt = 0;
        //字体初始化
        if (document == null) {
            //创建页面
            document = new PDDocument();
            fontConfig = new FontConfig(document);
            pageHeight = getTotalLength(model, modelSize);
            pageWidth = modelSize.width();
            float pageWidthPt = pageWidth * ptConvert;
            pageHeightPt = pageHeight * ptConvert;
            document = new PDDocument();
            page = new PDPage(new PDRectangle(pageWidthPt, pageHeightPt));
            document.addPage(page);
        } else {
            page = document.getPage(0);
        }
        //  创建内容流
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        //先绘制文字
        Field[] listField = model.getClass().getDeclaredFields();
        for (Field field : listField) {
            field.setAccessible(true);
            float xMM, yMM;
            if (field.isAnnotationPresent(PdfList.class)) {
                List<?> list = (List<?>) field.get(model);
                float moveY = minY;
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
                            PDFont pdFont = fontConfig.getPDFont(font.fontType());
                            for (String string : strings) {
                                xMM = getXMMbyPosition(position, font, pdFont, string);
                                if (!position.alignType().equals(AlignEnum.VERTICAL)) {
                                    yMM = moveY + getFontMM(font.fontSize());
                                    moveY += itemMargin + getFontMM(font.fontSize());
                                } else {
                                    yMM = (pageHeight - getFontMM(font.fontSize())) / 2;
                                }
                                drawText(contentStream, fontConfig, string, font, xMM * ptConvert, yMM * ptConvert);
                            }
                        }
                    }
                }
                //处理掉模版固定位置内容
            } else if (field.isAnnotationPresent(Position.class)) {
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

        // 10. 输出到响应（测试时保存到本地文件）
        String outputPath = "target/output.pdf";
        document.save(outputPath);
        document.close();
        System.out.println("PDF 已生成：" + outputPath);
        return outputPath;
    }

    public static float getTotalLength(Object model, ModelSize modelSize) throws IllegalAccessException, IOException {
        int sum = 0;
        //先处理外部模版
        if (modelSize == null || modelSize.height() == 0) {
            log.error("请使用正确的模版！");
            System.exit(1);
        }
        float height = modelSize.height(), white = height;
        //先求出空白部分高度
        Field[] listField = model.getClass().getDeclaredFields();
        for (Field field : listField) {
            if (field.isAnnotationPresent(PdfList.class)) {
                field.setAccessible(true);
                List<?> list = (List<?>) field.get(model);
                sum += getListLength(list);
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) genericType;
                    Type[] actualTypes = pt.getActualTypeArguments();
                    // 取第一个泛型参数，即 List<T> 中的 T
                    Class<?> elementClass = (Class<?>) actualTypes[0];
                    // 现在 elementClass 就是 List 里元素的类型，例如 CellInfo.class
                    // 你可以把它作为参数传递给解析方法
                    white -= getItemLength(elementClass);
                }
            }
        }
        sum += white;
        return sum;
    }

    //获取实际的列表高度,同时缓存字符串换行结果，这里不考虑递归情况
    public static float getListLength(List<?> list) throws IllegalAccessException, IOException {
        float len = 0;
        StringBuilder builder = new StringBuilder();
        for (Object o : list) {
            Field[] fields = o.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Position.class) && field.isAnnotationPresent(Font.class)
                        && StringUtils.isNotBlank((String) field.get(o))) {
                    Position position = field.getAnnotation(Position.class);
                    Font font = field.getAnnotation(Font.class);
                    String str = (String) field.get(o);
                    builder.setLength(0);
                    builder.append(position.title()).append(str);
                    String key = builder + "|" + font.fontType() + "|" + font.fontSize();
                    int fontSize = font.fontSize();
                    splitString(builder, key, font, 180);
                    len += strs.get(key).size() * getFontMM(fontSize) + itemMargin;
                }
            }
        }
        return len;
    }

    /**
     * 获取可变行元素的默认初始高度，也就是模版类中的列表所在类中，所有需要填充的文本行总高度
     */
    public static float getItemLength(Class<?> cls) {
        float yMin = Float.MAX_VALUE, yMax = 0;
        Field[] declaredFields = cls.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Position.class) && field.isAnnotationPresent(Font.class)) {
                field.setAccessible(true);
                Position position = field.getAnnotation(Position.class);
                Font font = field.getAnnotation(Font.class);
                int fontSize = font.fontSize();
                float move = getFontMM(fontSize);
                minY = Math.min(minY, position.positionY());
                yMin = Math.min(yMin, position.positionY());
                yMax = Math.max(yMax, position.positionY() + move);
            }
        }
        return yMax - yMin + itemMargin;
    }

    //文本换行并保存
    public static void splitString(StringBuilder text, String key, Font font, float maxWidthMM) throws IOException {
        // 2. 命中缓存则直接返回
        if (strs.containsKey(key)) {
            return;
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
                if (currentWidth + charWidth > getFountPt(maxWidthMM)) {
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
    }

    public static float getFontMM(int fontSize) {
        return fontSize * 25.4f / 72;
    }

    public static float getFountPt(float fontMM) {
        return fontMM * ptConvert;
    }

    //字符串颜色转Color对象
    private static Color parseColor(String colorHex) {
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
    public static void drawText(PDPageContentStream contentStream, FontConfig fontConfig, String text, Font font, float xPt, float yPt) throws IOException {
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
    public static void drawImage(PDDocument document, PDPageContentStream contentStream, String imagePath, float widthPt, float heightPt,
                                 float xPt, float yPt, float pageHeightPt) throws IOException {
        // 1. 从路径加载图片（支持本地路径、URL、ClassPath）
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
//        // 2. 从注解中取出宽度和高度（单位：毫米）
//        float widthMm = imageStyle.width();
//        float heightMm = imageStyle.height();
//
//        // 3. 毫米转点 (1 mm = 2.83464567 pt)
//        float widthPt = widthMm * ptConvert;
//        float heightPt = heightMm * ptConvert;

        // 4. 绘制图片（坐标原点在左下角）
        contentStream.drawImage(pdImage, xPt, pageHeightPt - yPt - heightPt, widthPt, heightPt);

    }

    private static float getXMMbyPosition(Position position, Font font, PDFont pdFont, String text) throws IOException {
        float xMM;
        if (!position.alignType().equals(AlignEnum.HORIZONTAL)) {
            xMM = position.positionX();
        } else {
            xMM = (pageWidth - pdFont.getStringWidth(text) / 1000 * font.fontSize() / ptConvert) / 2;
        }
        return xMM;
    }

    private static float getYMMbyPosition(Position position, Font font, float pageHeight) {
        float yMM;
        if (!position.alignType().equals(AlignEnum.VERTICAL)) {
            yMM = position.positionY();
        } else {
            yMM = (pageHeight - getFontMM(font.fontSize())) / 2;
        }
        return yMM;
    }

    public static PDDocument loadPdfTemplate(String path) throws IOException {
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


}

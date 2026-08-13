package pdf.config.style;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import pdf.enums.FontTypeEnum;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 11:04
 * @date 2026-05-12
 */
@Api(tags = "字体管理")
@Slf4j
public class FontConfig {

    private final Map<FontTypeEnum, PDFont> styleMap = new HashMap<>();

    public FontConfig(PDDocument document, Map<FontTypeEnum, TrueTypeFont> trueTypeFontMap) throws IOException {

            PDFont songtiFont = PDType0Font.load(document, trueTypeFontMap.get(FontTypeEnum.SONGTI),true);
            PDFont micFont = PDType0Font.load(document, trueTypeFontMap.get(FontTypeEnum.MICROSOFT_YAHEI),true);
            PDFont heitiFont = PDType0Font.load(document, trueTypeFontMap.get(FontTypeEnum.HEITI),true);
            // 初始化样式
            styleMap.put(FontTypeEnum.SONGTI, songtiFont);      // 宋体

            styleMap.put(FontTypeEnum.MICROSOFT_YAHEI, micFont);        // 微软雅黑

            styleMap.put(FontTypeEnum.HEITI, heitiFont);      // 黑体
    }

    public PDFont getPDFont(FontTypeEnum type) {
        return styleMap.get(type);
    }
}

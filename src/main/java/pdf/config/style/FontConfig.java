package pdf.config.style;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import pdf.enums.FontTypeEnum;

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

    public FontConfig(PDDocument document) {
        try (InputStream songtiFontStream = getClass().getResourceAsStream("/fonts/SimSun-01.ttf");
             InputStream micFontStream = getClass().getResourceAsStream("/fonts/msyh.ttf");
             InputStream heitiFontStream = getClass().getResourceAsStream("/fonts/simhei.ttf")) {
            PDFont songtiFont = PDType0Font.load(document, songtiFontStream);
            PDFont micFont = PDType0Font.load(document, micFontStream);
            PDFont heitiFont = PDType0Font.load(document, heitiFontStream);
            // 初始化样式
            styleMap.put(FontTypeEnum.SONGTI, songtiFont);      // 宋体

            styleMap.put(FontTypeEnum.MICROSOFT_YAHEI, micFont);        // 微软雅黑

            styleMap.put(FontTypeEnum.HEITI, heitiFont);      // 黑体
        } catch (Exception e) {
            log.error("生成出错");
            e.printStackTrace();
        }
    }

    public PDFont getPDFont(FontTypeEnum type) {
        return styleMap.get(type);
    }
}

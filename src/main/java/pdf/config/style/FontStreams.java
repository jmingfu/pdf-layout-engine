package pdf.config.style;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pdf.enums.FontTypeEnum;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-08-13 14:03
 * @date 2026-08-13
 */
@Configuration
public class FontStreams {
    @Bean
    public Map<FontTypeEnum, TrueTypeFont> initTrueTypeFont(){
        HashMap<FontTypeEnum, TrueTypeFont> map = new HashMap<>();
        try (InputStream songtiFontStream = getClass().getResourceAsStream("/fonts/SimSun-01.ttf");
             InputStream micFontStream = getClass().getResourceAsStream("/fonts/msyh.ttf");
             InputStream heitiFontStream = getClass().getResourceAsStream("/fonts/simhei.ttf")){
            map.put(FontTypeEnum.SONGTI,new TTFParser().parse(songtiFontStream));
            map.put(FontTypeEnum.MICROSOFT_YAHEI,new TTFParser().parse(micFontStream));
            map.put(FontTypeEnum.HEITI,new TTFParser().parse(heitiFontStream));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}

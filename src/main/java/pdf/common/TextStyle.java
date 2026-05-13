package pdf.common;

import io.swagger.annotations.Api;
import lombok.Data;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.*;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 11:12
 * @date 2026-05-12
 */
@Api(tags = "文本样式")
@Data
public class TextStyle {
    private PDFont font;      // PDFBox 字体对象
    private float fontSize;   // 字号
    private Color color;      // 颜色 (java.awt.Color)

}

package pdf.model;

import io.swagger.annotations.Api;
import lombok.Data;
import pdf.anno.ImageStyle;
import pdf.anno.ModelSize;
import pdf.anno.Position;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-16 11:45
 * @date 2026-05-16
 */
@Api(tags = "简历信息，这里我用来把简历头像改一改")
@Data
@ModelSize(height = 297)
public class Resume {
    @Position(positionX = 175,positionY = 8.5f,stringMargin = 0)
    @ImageStyle(width = 23,height = 31.2f)
    private String headImage;
}

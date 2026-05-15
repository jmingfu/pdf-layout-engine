package pdf.cell;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import pdf.anno.Font;
import pdf.anno.Position;
import pdf.enums.AlignEnum;
import pdf.enums.FontTypeEnum;

/**
 * 基于SpringBoot框架的个人练手项目-测试用细胞类
 *
 * @author JMF
 * @date 2026-05-12 09:41
 * @date 2026-05-12
 */
@Api(tags = "测试用细胞")
@Data
@ApiModel(description = "测试用细胞信息")
public class TestCell {
    private String repoName;

    @Position(positionY = 113, title = "细胞名称：", alignType = AlignEnum.HORIZONTAL)
    @Font(fontSize = 24, fontType = FontTypeEnum.MICROSOFT_YAHEI)
    private String cellName;

    @Position(positionY = 126, title = "存储日期：", alignType = AlignEnum.HORIZONTAL)
    @Font(fontSize = 18)
    private String cellDate;

    @Position(positionY = 136.82f, title = "存储方式：", alignType = AlignEnum.HORIZONTAL)
    @Font(fontSize = 12, fontType = FontTypeEnum.SONGTI, color = "#FF0000")
    private String cellWay;

}

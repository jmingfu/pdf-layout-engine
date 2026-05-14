package pdf.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import pdf.anno.*;
import pdf.cell.TestCell;
import pdf.enums.AlignEnum;
import pdf.enums.FontTypeEnum;
import pdf.enums.ItemTypeEnum;

import java.util.List;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 08:49
 * @date 2026-05-12
 */
@Data
@ModelSize(height = 240)
@ApiModel(description = "证书数据模型")
public class CertiModel {
    @Position(positionX = 2, positionY = 0)
    @ImageStyle(width = 206,height = 20)
    @ApiModelProperty("证书顶部图片url")
    private String topImg;

    @Position(positionX = 0, positionY = 0)
    @ImageStyle(width = 210,height = 2)
    @ApiModelProperty("证书顶部边框图片url")
    private String topLineImg;

    @Position(positionX = 0, positionY = 240, alignType = AlignEnum.SELF)
    @ImageStyle(width = 210,height = 2.2f)
    @ApiModelProperty("证书底框图片url")
    private String buttonLineImg;

    @Position(positionX = 0, positionY = 0, alignType = AlignEnum.SELF)
    @ImageStyle(width = 2)
    @ApiModelProperty("证书左边框图片url")
    private String leftLineImg;

    @Position(positionX = 208f, positionY = 0, alignType = AlignEnum.SELF)
    @ImageStyle(width = 2.2f)
    @ApiModelProperty("证书右边框图片url")
    private String rightLineImg;

    @Position(positionX = 123, positionY = 225, alignType = AlignEnum.SELF)
    @Font(fontSize = 16)
    @ApiModelProperty("组织名称")
    private String orgName;

    @Position(positionY = 70, alignType = AlignEnum.HORIZONTAL)
    @Font(fontType = FontTypeEnum.HEITI, fontSize = 32)
    @ApiModelProperty("证书标题")
    private String certiTitle;

    @ApiModelProperty("细胞列表")
    @Position(positionY = 113)
    @PdfList
    List<TestCell> cellList;

    @Position(positionX = 164, positionY = 214, title = "储存人姓名：", alignType = AlignEnum.SELF)
    @Font(fontSize = 18)
    private String repoName;

}

package pdf.anno;


import io.swagger.annotations.Api;
import pdf.enums.AlignEnum;
import pdf.enums.ItemTypeEnum;

import java.lang.annotation.*;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 08:50
 * @date 2026-05-12
 */
@Api("元素坐标，单位mm")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface Position {
    float positionX() default -1;//距离pdf最左侧长度，单位mm
    float positionY() default -1;//距离pdf顶部长度
    String title() default "";//文字的固定字段，例如“姓名：张三”里的“姓名：”
    AlignEnum alignType() default AlignEnum.DEFAULT;//居中类型，HORIZONTAL-横向居中。VERTICAL-纵向。DEFAULT-原始坐标。SELF-自定义，引擎不绘制该元素。
    float stringMargin() default 4.5f;//行间距，文字底部距离下一行顶部距离
}

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
    float positionX() default -1;
    float positionY() default -1;
    String title() default "";
    AlignEnum alignType() default AlignEnum.DEFAULT;
}

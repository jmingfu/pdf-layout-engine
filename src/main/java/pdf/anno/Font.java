package pdf.anno;

import io.swagger.annotations.Api;
import pdf.enums.FontTypeEnum;

import java.lang.annotation.*;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 10:10
 * @date 2026-05-12
 */
@Api(tags = "字体设置")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface Font {
    FontTypeEnum fontType() default FontTypeEnum.MICROSOFT_YAHEI;
    int fontSize();
    String color() default "#000000";
}

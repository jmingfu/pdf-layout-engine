package pdf.anno;

import io.swagger.annotations.Api;

import java.lang.annotation.*;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-13 11:18
 * @date 2026-05-13
 */
@Api(tags = "图片样式")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface ImageStyle {
    float width() default 0;
    float height() default 0;
}

package pdf.anno;

import io.swagger.annotations.Api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 12:30
 * @date 2026-05-12
 */
@Api(tags = "模版尺寸")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ModelSize {
    float width() default 210;
    float height();
}

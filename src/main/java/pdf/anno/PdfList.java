package pdf.anno;

import io.swagger.annotations.Api;

import java.lang.annotation.*;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 11:38
 * @date 2026-05-12
 */
@Api(tags = "是否需要填充的列表标记")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface PdfList {
    float listMargin() default 4.5f; //列表间距
}

package pdf.enums;

import io.swagger.annotations.Api;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 09:57
 * @date 2026-05-12
 */
@Api(tags = "居中类型，HORIZONTAL-横向居中。DEFAULT-不做处理。SELF-自定义")
public enum AlignEnum {
    HORIZONTAL,
    DEFAULT,
    SELF
}

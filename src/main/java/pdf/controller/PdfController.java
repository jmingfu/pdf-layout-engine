package pdf.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pdf.anno.Position;
import pdf.cell.TestCell;
import pdf.service.PdfService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 10:46
 * @date 2026-05-12
 */
@Api(tags = "pdf控制器")
@RestController()
@RequestMapping("/pdf")
public class PdfController {
    @Autowired
    private PdfService pdfService;

    @ApiOperation(value = "生成证书", notes = "根据用户ID生成PDF证书并返回文件流")
    @PostMapping("/ByObj")
    public String generateCertificate() throws Exception {
        return pdfService.generateMyCerti();
    }

    @ApiOperation(value = "生成简历", notes = "测试简历图片位置切换")
    @PostMapping("/ByPath")
    public void generateResume() throws Exception {
        pdfService.generateResume();
    }
}

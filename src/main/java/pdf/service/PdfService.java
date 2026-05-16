package pdf.service;

import io.swagger.annotations.Api;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import pdf.cell.TestCell;

import java.io.IOException;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 10:55
 * @date 2026-05-12
 */
@Api(tags = "pdf服务类")

public interface PdfService {
    byte[] generateCertificate(TestCell testCell);

    void generateMyCerti() throws IOException, IllegalAccessException, NoSuchFieldException;

    void generateResume() throws IOException, IllegalAccessException;
}

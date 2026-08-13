package pdf.service;

import io.swagger.annotations.Api;
import pdf.cell.TestCell;

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

    String generateMyCerti() throws Exception;

    void generateResume() throws Exception;

    void batchGenerateCertificate();
}

package pdf.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import pdf.anno.Font;
import pdf.anno.ImageStyle;
import pdf.anno.ModelSize;
import pdf.anno.Position;
import pdf.cell.TestCell;
import pdf.common.PdfUtils;
import pdf.config.style.FontConfig;
import pdf.model.CertiModel;
import pdf.model.Resume;
import pdf.service.PdfService;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于SpringBoot框架的个人练手项目-
 *
 * @author JMF
 * @date 2026-05-12 11:00
 * @date 2026-05-12
 */
@Service
@Slf4j
public class PdfServiceImpl implements PdfService {
    PdfUtils pdfUtils = new PdfUtils();

    @Override
    public byte[] generateCertificate(TestCell testCell) {
        return null;
    }

    @Override
    public String generateMyCerti() throws Exception {
        // 1. 准备测试数据
        TestCell cell1 = new TestCell();
        cell1.setRepoName("王五");
        cell1.setCellName("干细胞-001");
        cell1.setCellDate("2026-05-12");
        cell1.setCellWay("液氮冷冻");

        TestCell cell2 = new TestCell();
        cell2.setRepoName("王五");
        cell2.setCellName("免疫细胞-002");
        cell2.setCellDate("2026-05-11");
        cell2.setCellWay("超低温保存");

        // 模拟长文本换行测试
        TestCell cell3 = new TestCell();
        cell3.setRepoName("王五");
        cell3.setCellName("这是一个非常非常长的细胞名称用来测试换行逻辑是否能正常工作");
//        cell3.setCellDate("2026-05-10");
        cell3.setCellWay("常规冷冻");

        // 2. 构建模板对象
        CertiModel model = new CertiModel();
        model.setTopImg("/images/顶部图片.png");
        model.setTopLineImg("/images/边框图片.png");
        model.setButtonLineImg("/images/边框图片.png");
        model.setLeftLineImg("/images/边框图片.png");
        model.setRightLineImg("/images/边框图片.png");
        model.setOrgName("XX生物科技有限公司");
        model.setCertiTitle("细胞储存证书");
        model.setRepoName("王五");
        // 添加多个细胞测试高度累加
        List<TestCell> cellList = new ArrayList<>();
//        cellList.add(cell1);
        cellList.add(cell2);
        cellList.add(cell3);
        model.setCellList(cellList);

        // 3. 打印测试信息
        System.out.println("========== 测试数据 ==========");
        System.out.println("细胞数量: " + cellList.size());
        System.out.println();

        // 单个细胞内容预览
        for (int i = 0; i < cellList.size(); i++) {
            TestCell cell = cellList.get(i);
            System.out.println("细胞" + (i + 1) + ":");
            System.out.println("  储存人: " + cell.getRepoName());
            System.out.println("  细胞名称: " + cell.getCellName());
            System.out.println("  存储日期: " + cell.getCellDate());
            System.out.println("  存储方式: " + cell.getCellWay());
            //System.out.println("  细胞名称长度: " + cell.getCellName().length() + " 字符");
            System.out.println();
        }
        //6. 调用生成方法
        //7接下来就需要自定义的后置处理。例如引擎无法处理的右下角跟随文字移动的用户名、公司名；底框。还有需要拉伸的侧边框
        PDDocument document = pdfUtils.generatePdfDocument(model);
        // 3. 如果都打不开
        if (document == null) {
            log.error("引擎处理pdf出错，文件不存在");
            return "";
        }
        //自定义后置处理
        return postChange(document, model);
        // 10. 输出到响应（测试时保存到本地文件）
//        String outputPath = "target/output.pdf";
//        document.save(outputPath);
//        document.close();
//        System.out.println("PDF 已生成：" + outputPath);
    }

    public String postChange(PDDocument document, CertiModel model) throws IOException, NoSuchFieldException, IllegalAccessException {
        float ptCv = pdfUtils.ptConvert;
        PDPage page = document.getPage(0);
        PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
        //分别绘制底部边框图片，左边框图片，有边框图片。
        Field buttonLineImg = model.getClass().getDeclaredField("buttonLineImg");
        buttonLineImg.setAccessible(true);
        float heightPt = page.getMediaBox().getHeight();
        ImageStyle imageStyle = buttonLineImg.getAnnotation(ImageStyle.class);
        Position position;
        String path = (String) buttonLineImg.get(model);
        String normalizedPath = path;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        InputStream imageStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(normalizedPath);
        if (imageStream == null) {
            throw new IOException("图片资源不存在: " + path);
        }
        byte[] imageBytes = IOUtils.toByteArray(imageStream);
        imageStream.close();
        // 2. 从 InputStream 创建 PDImageXObject
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, path);
        contentStream.drawImage(pdImage, 0, -0.2f, imageStyle.width() * ptCv, imageStyle.height() * ptCv);
        Field leftLineImg = model.getClass().getDeclaredField("leftLineImg");
        drawTwoSide(document, model, page, contentStream, leftLineImg);
        Field rightLineImg = model.getClass().getDeclaredField("rightLineImg");
        drawTwoSide(document, model, page, contentStream, rightLineImg);
        //绘制剩余文字
        Field orgName = model.getClass().getDeclaredField("orgName");
        orgName.setAccessible(true);
        Font font = orgName.getAnnotation(Font.class);
        position = orgName.getAnnotation(Position.class);
        String text = (String) orgName.get(model);
        FontConfig fontConfig = new FontConfig(document);
        int len = text.length();
        float strWidthPt = 0;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            strWidthPt += fontConfig.getPDFont(font.fontType()).getStringWidth(String.valueOf(c)) / 1000f * font.fontSize();
        }
        ModelSize modelSize = model.getClass().getAnnotation(ModelSize.class);
        pdfUtils.drawText(contentStream, fontConfig, text, font, (modelSize.width() - 15) * ptCv - strWidthPt,
                heightPt - (modelSize.height() - position.positionY()) * ptCv);
        Field repoName = model.getClass().getDeclaredField("repoName");
        repoName.setAccessible(true);
        font = repoName.getAnnotation(Font.class);
        position = repoName.getAnnotation(Position.class);
        text = repoName.get(model) + position.title();
        len = text.length();
        strWidthPt = 0;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            strWidthPt += fontConfig.getPDFont(font.fontType()).getStringWidth(String.valueOf(c)) / 1000f * font.fontSize();
        }
        pdfUtils.drawText(contentStream, fontConfig, text, font, (modelSize.width() - 15) * ptCv - strWidthPt,
                heightPt - (modelSize.height() - position.positionY()) * ptCv);
        contentStream.close();

        // 10. 输出到响应（测试时保存到本地文件）
        String outputPath = "target/output.pdf";
        document.save(outputPath);
        document.close();
        System.out.println("PDF 已生成：" + outputPath);
        return outputPath;
    }

    private void drawTwoSide(PDDocument document, CertiModel model, PDPage page, PDPageContentStream contentStream, Field rightLineImg) throws IllegalAccessException, IOException {
        ImageStyle imageStyle;
        Position position;
        String path;
        rightLineImg.setAccessible(true);
        imageStyle = rightLineImg.getAnnotation(ImageStyle.class);
        position = rightLineImg.getAnnotation(Position.class);
        path = (String) rightLineImg.get(model);
        pdfUtils.drawImage(document, contentStream, path, imageStyle.width() * pdfUtils.ptConvert,
                page.getMediaBox().getHeight(), position.positionX() * pdfUtils.ptConvert, 0, page.getMediaBox().getHeight());
    }

    public void generateResume() throws Exception {
        Resume resume = new Resume();
        resume.setHeadImage("images/简历头像.jpg");
        PDDocument document = pdfUtils.modifyPdfDocument(resume, "pdfs/Resume.pdf");
        String outputPath = "target/output.pdf";
        document.save(outputPath);
        document.close();
        System.out.println("PDF 已生成：" + outputPath);

    }
}

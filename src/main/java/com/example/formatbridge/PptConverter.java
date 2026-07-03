package com.example.formatbridge;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class PptConverter {

    // PPT → PDF
    public static void pptToPdf(String inputPath, String outputPath) throws IOException {
        System.out.println("📄 开始 PPT → PDF 转换...");

        try (FileInputStream fis = new FileInputStream(inputPath);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {

            PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H",
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

            int slideIndex = 0;
            for (XSLFSlide slide : ppt.getSlides()) {
                slideIndex++;

                // 添加页码标题
                document.add(new Paragraph("第 " + slideIndex + " 页").setFont(font).setFontSize(14));

                // 提取文本内容 - 遍历所有形状
                StringBuilder content = new StringBuilder();
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        String text = textShape.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            content.append(text).append("\n");
                        }
                    }
                }

                if (content.length() > 0) {
                    document.add(new Paragraph(content.toString()).setFont(font).setFontSize(12));
                } else {
                    document.add(new Paragraph("（此页无文本内容）").setFont(font).setFontSize(12));
                }

                document.add(new Paragraph(" "));
            }

            document.close();
            System.out.println("✅ PPT → PDF 转换成功，共 " + slideIndex + " 页");
        }
    }
}
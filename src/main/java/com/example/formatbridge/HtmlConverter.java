package com.example.formatbridge;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HtmlConverter {

    // HTML → PDF
    public static void htmlToPdf(String inputPath, String outputPath) throws IOException {
        System.out.println("📄 开始 HTML → PDF 转换...");

        String content = new String(Files.readAllBytes(Paths.get(inputPath)), "UTF-8");

        PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H",
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

        // 简单提取文本（去除HTML标签）
        String plainText = content
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();

        String[] lines = plainText.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                document.add(new Paragraph(line.trim()).setFont(font).setFontSize(12));
            }
        }

        document.close();
        System.out.println("✅ HTML → PDF 转换成功");
    }

    // PDF → HTML
    public static void pdfToHtml(String inputPath, String outputPath) throws IOException {
        System.out.println("📄 开始 PDF → HTML 转换...");

        try (PDDocument pdfDoc = PDDocument.load(new java.io.File(inputPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDoc);

            String html = "<html><head><meta charset='UTF-8'></head><body><pre>";
            html += text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            html += "</pre></body></html>";

            Files.write(Paths.get(outputPath), html.getBytes("UTF-8"));
            System.out.println("✅ PDF → HTML 转换成功");
        }
    }
}
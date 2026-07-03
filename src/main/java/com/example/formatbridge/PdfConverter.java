package com.example.formatbridge;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfConverter {

    /**
     * TXT 转 PDF
     */
    public static void txtToPdf(String inputPath, String outputPath) throws IOException {
        // 1. 读取 TXT 内容
        String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(inputPath)));

        // 2. 创建 PDF
        PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // 3. 把文字写入 PDF
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                document.add(new Paragraph(" "));
            } else {
                document.add(new Paragraph(line));
            }
        }

        // 4. 关闭
        document.close();
        System.out.println("✅ TXT → PDF 转换成功：" + outputPath);
    }

    /**
     * PDF 转 TXT
     */
    public static void pdfToTxt(String inputPath, String outputPath) throws IOException {
        // 用 PDFBox 读取 PDF
        org.apache.pdfbox.pdmodel.PDDocument pdfDoc = org.apache.pdfbox.pdmodel.PDDocument.load(new java.io.File(inputPath));
        org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
        String text = stripper.getText(pdfDoc);
        pdfDoc.close();

        // 保存为 TXT
        java.nio.file.Files.write(java.nio.file.Paths.get(outputPath), text.getBytes());
        System.out.println("✅ PDF → TXT 转换成功：" + outputPath);
    }
}
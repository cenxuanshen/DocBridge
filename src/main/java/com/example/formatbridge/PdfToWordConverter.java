package com.example.formatbridge;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.FileOutputStream;
import java.io.IOException;

public class PdfToWordConverter {

    // PDF → Word（纯文本提取，不处理图片）
    public static void convert(String inputPath, String outputPath) throws IOException {
        System.out.println("📄 开始 PDF → Word 转换（纯文本）...");

        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(inputPath));
             XWPFDocument wordDoc = new XWPFDocument()) {

            int pageCount = pdfDoc.getNumberOfPages();

            for (int i = 1; i <= pageCount; i++) {
                // 提取每页文本
                String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));

                if (pageText != null && !pageText.trim().isEmpty()) {
                    // 分页标记
                    if (i > 1) {
                        XWPFParagraph para = wordDoc.createParagraph();
                        XWPFRun run = para.createRun();
                        run.setText("================ 第 " + i + " 页 ================");
                        run.setFontSize(12);
                        run.setFontFamily("宋体");
                    }

                    // 按行写入
                    String[] lines = pageText.split("\n");
                    for (String line : lines) {
                        if (line.trim().isEmpty()) continue;
                        XWPFParagraph para = wordDoc.createParagraph();
                        XWPFRun run = para.createRun();
                        run.setText(line);
                        run.setFontSize(12);
                        run.setFontFamily("宋体");
                    }
                }
            }

            try (FileOutputStream out = new FileOutputStream(outputPath)) {
                wordDoc.write(out);
            }
            wordDoc.close();
            System.out.println("✅ PDF → Word 转换成功（共 " + pageCount + " 页）");
        } catch (Exception e) {
            System.err.println("❌ 转换失败: " + e.getMessage());
            e.printStackTrace();
            throw new IOException(e);
        }
    }
}
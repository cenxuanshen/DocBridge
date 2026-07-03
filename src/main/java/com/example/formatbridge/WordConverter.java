package com.example.formatbridge;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class WordConverter {

    // ===== Word → PDF =====
    public static void wordToPdf(String inputPath, String outputPath) throws IOException {
        System.out.println("📄 开始 Word → PDF 转换...");

        try (FileInputStream fis = new FileInputStream(inputPath);
             XWPFDocument wordDoc = new XWPFDocument(fis)) {

            PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H",
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

            for (XWPFParagraph para : wordDoc.getParagraphs()) {
                Paragraph pdfPara = new Paragraph();
                pdfPara.setFont(font);

                if (para.getAlignment() != null) {
                    switch (para.getAlignment()) {
                        case CENTER:
                            pdfPara.setTextAlignment(TextAlignment.CENTER);
                            break;
                        case RIGHT:
                            pdfPara.setTextAlignment(TextAlignment.RIGHT);
                            break;
                        case BOTH:
                            pdfPara.setTextAlignment(TextAlignment.JUSTIFIED);
                            break;
                        default:
                            pdfPara.setTextAlignment(TextAlignment.LEFT);
                    }
                }

                for (XWPFRun run : para.getRuns()) {
                    String text = run.getText(0);
                    if (text == null || text.isEmpty()) continue;

                    Text pdfText = new Text(text);
                    pdfText.setFont(font);

                    if (run.isBold()) pdfText.setBold();
                    if (run.isItalic()) pdfText.setItalic();
                    if (run.getUnderline() != UnderlinePatterns.NONE) {
                        pdfText.setUnderline();
                    }
                    if (run.isStrikeThrough()) {
                        pdfText.setUnderline();
                    }

                    Double fontSizeObj = run.getFontSizeAsDouble();
                    float fontSize = 12f;
                    if (fontSizeObj != null && fontSizeObj > 0) {
                        fontSize = fontSizeObj.floatValue();
                    }
                    pdfText.setFontSize(fontSize);

                    String colorHex = run.getColor();
                    if (colorHex != null && colorHex.length() == 6) {
                        try {
                            int r = Integer.parseInt(colorHex.substring(0, 2), 16);
                            int g = Integer.parseInt(colorHex.substring(2, 4), 16);
                            int b = Integer.parseInt(colorHex.substring(4, 6), 16);
                            pdfText.setFontColor(new DeviceRgb(r, g, b));
                        } catch (Exception e) { /* 忽略 */ }
                    }

                    int textPos = run.getTextPosition();
                    if (textPos == 1) {
                        pdfText.setTextRise(6);
                        pdfText.setFontSize(fontSize * 0.7f);
                    } else if (textPos == -1) {
                        pdfText.setTextRise(-3);
                        pdfText.setFontSize(fontSize * 0.7f);
                    }

                    pdfPara.add(pdfText);
                }

                if (!pdfPara.getChildren().isEmpty()) {
                    document.add(pdfPara);
                }
            }

            // 处理图片
            int imageCount = 0;
            for (XWPFParagraph para : wordDoc.getParagraphs()) {
                for (XWPFRun run : para.getRuns()) {
                    List<XWPFPicture> pictures = run.getEmbeddedPictures();
                    for (XWPFPicture picture : pictures) {
                        XWPFPictureData pictureData = picture.getPictureData();
                        if (pictureData == null) continue;

                        byte[] imageBytes = pictureData.getData();
                        if (imageBytes == null || imageBytes.length == 0) continue;

                        try {
                            com.itextpdf.io.image.ImageData imageData =
                                    com.itextpdf.io.image.ImageDataFactory.create(imageBytes);
                            Image pdfImage = new Image(imageData);

                            float maxWidth = 500;
                            if (pdfImage.getImageWidth() > maxWidth) {
                                float scale = maxWidth / pdfImage.getImageWidth();
                                pdfImage.setWidth(maxWidth);
                                pdfImage.setHeight(pdfImage.getImageHeight() * scale);
                            }
                            document.add(pdfImage);
                            imageCount++;
                        } catch (Exception e) {
                            System.out.println("   ⚠️ 图片解析失败: " + e.getMessage());
                        }
                    }
                }
            }
            System.out.println("📸 共提取 " + imageCount + " 张图片");

            // 处理表格
            for (XWPFTable table : wordDoc.getTables()) {
                List<XWPFTableRow> rows = table.getRows();
                if (rows.isEmpty()) continue;

                int colCount = 0;
                for (XWPFTableRow row : rows) {
                    colCount = Math.max(colCount, row.getTableCells().size());
                }
                if (colCount == 0) colCount = 2;

                Table pdfTable = new Table(UnitValue.createPercentArray(colCount));
                pdfTable.setWidth(UnitValue.createPercentValue(100));

                for (XWPFTableRow row : rows) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        Cell pdfCell = new Cell().add(new Paragraph(cellText).setFont(font));
                        pdfTable.addCell(pdfCell);
                    }
                    int cellCount = row.getTableCells().size();
                    for (int i = cellCount; i < colCount; i++) {
                        pdfTable.addCell(new Cell().add(new Paragraph(" ").setFont(font)));
                    }
                }

                document.add(pdfTable);
            }

            document.close();
            System.out.println("✅ Word → PDF 转换成功");
        }
    }

    // ===== PDF → Word（保留图片） =====
    public static void pdfToWord(String inputPath, String outputPath) throws IOException {
        System.out.println("📄 开始 PDF → Word 转换（保留图片）...");

        try (PDDocument pdfDoc = PDDocument.load(new java.io.File(inputPath));
             XWPFDocument wordDoc = new XWPFDocument()) {

            // 1. 提取文本
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDoc);

            String[] lines = text.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                XWPFParagraph para = wordDoc.createParagraph();
                XWPFRun run = para.createRun();
                run.setText(line);
                run.setFontSize(12);
                run.setFontFamily("宋体");
            }

            // 2. 提取图片（每页渲染为图片插入）
            int imageCount = 0;
            PDFRenderer renderer = new PDFRenderer(pdfDoc);

            for (int page = 0; page < pdfDoc.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImage(page, 1.0f);

                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                byte[] imageBytes = baos.toByteArray();

                // 创建段落，通过 run.addPicture 插入图片
                XWPFParagraph para = wordDoc.createParagraph();
                XWPFRun run = para.createRun();
                run.addPicture(
                        new ByteArrayInputStream(imageBytes),
                        XWPFDocument.PICTURE_TYPE_PNG,
                        "image_page_" + (page + 1) + ".png",
                        image.getWidth() / 10,
                        image.getHeight() / 10
                );
                imageCount++;
            }

            System.out.println("📸 共提取 " + imageCount + " 张图片（每页一图）");

            try (FileOutputStream out = new FileOutputStream(outputPath)) {
                wordDoc.write(out);
            }
            wordDoc.close();
            System.out.println("✅ PDF → Word 转换成功（含图片）");
        } catch (Exception e) {
            System.err.println("❌ PDF → Word 转换失败: " + e.getMessage());
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    // ===== 提取 PDF 文本（备用） =====
    private static String extractTextFromPdf(String filePath) throws IOException {
        try (com.itextpdf.kernel.pdf.PdfDocument pdfDoc =
                     new com.itextpdf.kernel.pdf.PdfDocument(
                             new com.itextpdf.kernel.pdf.PdfReader(filePath))) {

            StringBuilder text = new StringBuilder();

            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
                if (pageText != null && !pageText.isEmpty()) {
                    text.append(pageText).append("\n");
                }
            }

            return text.toString();
        } catch (Exception e) {
            System.err.println("❌ iText 读取 PDF 失败: " + e.getMessage());
            return "";
        }
    }
}
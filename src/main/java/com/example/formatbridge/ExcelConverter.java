package com.example.formatbridge;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelConverter {

    public static void excelToPdf(String inputPath, String outputPath) throws IOException {
        System.out.println("📄 开始 Excel → PDF 转换...");

        try (FileInputStream fis = new FileInputStream(inputPath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H",
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);

                Text titleText = new Text("Sheet: " + sheet.getSheetName());
                titleText.setFont(font);
                titleText.setFontSize(14);
                titleText.setBold();
                Paragraph titlePara = new Paragraph().add(titleText);
                document.add(titlePara);

                int colCount = 0;
                for (Row row : sheet) {
                    colCount = Math.max(colCount, row.getPhysicalNumberOfCells());
                }
                if (colCount == 0) colCount = 5;

                Table pdfTable = new Table(UnitValue.createPercentArray(colCount));
                pdfTable.setWidth(UnitValue.createPercentValue(100));

                for (Row row : sheet) {
                    for (int c = 0; c < colCount; c++) {
                        org.apache.poi.ss.usermodel.Cell poiCell = row.getCell(c);
                        String cellValue = getCellValue(poiCell);
                        Cell pdfCell = new Cell().add(new Paragraph(cellValue).setFont(font));
                        pdfTable.addCell(pdfCell);
                    }
                }

                document.add(pdfTable);
                document.add(new Paragraph(" "));
            }

            document.close();
            System.out.println("✅ Excel → PDF 转换成功");
        }
    }

    private static String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
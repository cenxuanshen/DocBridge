package com.example.formatbridge;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@RestController
public class ConvertController {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @PostMapping("/convert")
    public ResponseEntity<byte[]> convert(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fromFormat") String fromFormat,
            @RequestParam("toFormat") String toFormat) throws IOException {

        System.out.println("========================================");
        long maxSize = 50 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            String errorMsg = "文件过大！当前文件 " + (file.getSize() / 1024 / 1024) + "MB，最大支持 50MB";
            System.out.println("❌ " + errorMsg);
            return ResponseEntity.badRequest().body(errorMsg.getBytes());
        }
        System.out.println("========================================");

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空".getBytes());
        }

        String originalFilename = file.getOriginalFilename();
        String baseName = originalFilename != null ? originalFilename.replaceFirst("\\.[^.]+$", "") : "file";
        String inputPath = TEMP_DIR + File.separator + UUID.randomUUID() + "_" + baseName + "." + fromFormat;
        String outputPath = TEMP_DIR + File.separator + UUID.randomUUID() + "_" + baseName + "." + toFormat;

        File inputFile = new File(inputPath);
        try (FileOutputStream fos = new FileOutputStream(inputFile)) {
            fos.write(file.getBytes());
        }
        System.out.println("✅ 文件已保存到临时目录");

        try {
            if ("txt".equals(fromFormat) && "pdf".equals(toFormat)) {
                return ResponseEntity.badRequest()
                        .body("暂不支持 TXT → PDF".getBytes());

            } else if ("docx".equals(fromFormat) && "pdf".equals(toFormat)) {
                // Word → PDF（调用升级后的转换器）
                System.out.println("📄 开始 Word → PDF 转换...");
                WordConverter.wordToPdf(inputPath, outputPath);
                System.out.println("✅ Word → PDF 转换成功");

            } else if ("pdf".equals(fromFormat) && "docx".equals(toFormat)) {
                System.out.println("📄 开始 PDF → Word 转换...");
                PdfToWordConverter.convert(inputPath, outputPath);
                System.out.println("✅ PDF → Word 转换成功");

            } else if ("xlsx".equals(fromFormat) && "pdf".equals(toFormat)) {
                System.out.println("📄 开始 Excel → PDF 转换...");
                ExcelConverter.excelToPdf(inputPath, outputPath);
                System.out.println("✅ Excel → PDF 转换成功");

            } else if ("pdf".equals(fromFormat) && "xlsx".equals(toFormat)) {
                return ResponseEntity.badRequest()
                        .body("暂不支持 PDF → Excel".getBytes());
            } else if ("html".equals(fromFormat) && "pdf".equals(toFormat)) {
                System.out.println("📄 开始 HTML → PDF 转换...");
                HtmlConverter.htmlToPdf(inputPath, outputPath);
                System.out.println("✅ HTML → PDF 转换成功");

            } else if ("pdf".equals(fromFormat) && "html".equals(toFormat)) {
                System.out.println("📄 开始 PDF → HTML 转换...");
                HtmlConverter.pdfToHtml(inputPath, outputPath);
                System.out.println("✅ PDF → HTML 转换成功");
            } else if ("pptx".equals(fromFormat) && "pdf".equals(toFormat)) {
                System.out.println("📄 开始 PPT → PDF 转换...");
                PptConverter.pptToPdf(inputPath, outputPath);
                System.out.println("✅ PPT → PDF 转换成功");
            } else {
                System.out.println("⚠️ 暂不支持: " + fromFormat + " → " + toFormat);
                return ResponseEntity.badRequest()
                        .body(("暂不支持 " + fromFormat + " → " + toFormat + " 转换").getBytes());
            }

            byte[] outputBytes = Files.readAllBytes(new File(outputPath).toPath());
            System.out.println("📤 返回文件大小: " + outputBytes.length + " 字节");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String encodedFileName = java.net.URLEncoder.encode(baseName, "UTF-8").replace("+", "%20");
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "." + toFormat + "\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(outputBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(("转换失败: " + e.getMessage()).getBytes());
        } finally {
            if (inputFile.exists()) inputFile.delete();
            new File(outputPath).delete();
        }
    }
}
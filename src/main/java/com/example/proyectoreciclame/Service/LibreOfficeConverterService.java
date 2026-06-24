package com.example.proyectoreciclame.Service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class LibreOfficeConverterService {

    private static final Logger log = LoggerFactory.getLogger(LibreOfficeConverterService.class);

    private static final String[] SOFFICE_CANDIDATES = {
        "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
        "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe",
        "/usr/bin/soffice",
        "/usr/local/bin/soffice",
        "soffice"
    };

    private String resolveSoffice() {
        for (String path : SOFFICE_CANDIDATES) {
            if (new java.io.File(path).exists()) return path;
        }
        return "soffice";
    }

    /**
     * Convierte PPTX a PDF usando LibreOffice headless.
     * Más rápido que Aspose y sin marca de agua de evaluación.
     */
    public byte[] convertPptxToPdf(byte[] pptxData) throws Exception {
        Path tempDir = Files.createTempDirectory("lo_conv_");
        Path inputFile = tempDir.resolve("input.pptx");
        Files.write(inputFile, pptxData);

        try {
            String soffice = resolveSoffice();
            log.info("[LibreOffice] Usando: {}", soffice);

            ProcessBuilder pb = new ProcessBuilder(
                soffice,
                "--headless",
                "--convert-to", "pdf",
                "--outdir", tempDir.toString(),
                inputFile.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("LibreOffice tardó más de 120 segundos y fue cancelado");
            }
            if (process.exitValue() != 0) {
                throw new RuntimeException("LibreOffice falló (exit " + process.exitValue() + "): " + output);
            }

            Path outputPdf = tempDir.resolve("input.pdf");
            if (!Files.exists(outputPdf)) {
                throw new RuntimeException("LibreOffice no generó el PDF de salida. Output: " + output);
            }

            byte[] pdfBytes = Files.readAllBytes(outputPdf);
            log.info("[LibreOffice] Conversión OK: {} bytes", pdfBytes.length);
            return pdfBytes;

        } finally {
            // Limpiar archivos temporales
            try { Files.deleteIfExists(inputFile); } catch (IOException ignored) {}
            try { Files.deleteIfExists(tempDir.resolve("input.pdf")); } catch (IOException ignored) {}
            try { Files.deleteIfExists(tempDir); } catch (IOException ignored) {}
        }
    }

    /**
     * Añade marca de agua diagonal "RECICLAME" al PDF usando PDFBox.
     */
    public byte[] addWatermark(byte[] pdfData) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfData)) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PDExtendedGraphicsState transparencyState = new PDExtendedGraphicsState();
            transparencyState.setNonStrokingAlphaConstant(0.08f);
            transparencyState.setStrokingAlphaConstant(0.08f);
            transparencyState.setAlphaSourceFlag(true);

            for (PDPage page : doc.getPages()) {
                float pageWidth  = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();

                try (PDPageContentStream cs = new PDPageContentStream(
                        doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    cs.setGraphicsStateParameters(transparencyState);
                    cs.setFont(font, 72);
                    cs.setNonStrokingColor(0f, 109f / 255f, 55f / 255f); // verde Reciclame

                    // Centrar diagonal
                    float angle = (float) Math.toRadians(45);
                    float cx = pageWidth  / 2f - 120f;
                    float cy = pageHeight / 2f - 30f;

                    cs.beginText();
                    cs.setTextMatrix(Matrix.getRotateInstance(angle, cx, cy));
                    cs.showText("RECICLAME");
                    cs.endText();
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.save(bos);
            return bos.toByteArray();
        }
    }

    /**
     * Pipeline completo: PPTX → PDF (LibreOffice) → watermark (PDFBox).
     */
    public byte[] convertAndWatermark(byte[] pptxData) throws Exception {
        byte[] pdfData = convertPptxToPdf(pptxData);
        return addWatermark(pdfData);
    }
}

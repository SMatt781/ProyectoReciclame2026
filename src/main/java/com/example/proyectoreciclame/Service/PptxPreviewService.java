package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Repository.EstudioRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class PptxPreviewService {

    private static final Logger log = LoggerFactory.getLogger(PptxPreviewService.class);

    @Value("${libreoffice.path:soffice}")
    private String libreOfficePath;

    @Value("${libreoffice.timeout-seconds:120}")
    private int libreOfficeTimeout;

    @Autowired
    private S3StorageService s3StorageService;

    @Autowired
    private EstudioRepository estudioRepository;

    /**
     * Genera el PDF de vista previa en segundo plano.
     * Convierte PPTX → PDF con LibreOffice, añade marca de agua "RECICLAME"
     * con PDFBox y sube el resultado a S3.
     */
    @Async("pptxPreviewExecutor")
    public void generarPreviewAsync(Long estudioId) {
        try {
            Estudio estudio = estudioRepository.findById(estudioId).orElse(null);
            if (estudio == null || estudio.getArchivoUrl() == null || estudio.getArchivoUrl().isBlank()) {
                log.warn("[PREVIEW] Estudio {} no encontrado o sin archivo", estudioId);
                return;
            }
            if (estudio.getFormato() != Estudio.FormatoEstudio.PPTX) {
                return;
            }

            log.info("[PREVIEW] Iniciando generación de preview para estudio id={}", estudioId);
            long t0 = System.currentTimeMillis();

            byte[] pptxBytes = s3StorageService.downloadFile(estudio.getArchivoUrl());
            log.info("[PREVIEW] PPTX descargado: {} bytes", pptxBytes.length);

            byte[] pdfBytes = convertirConLibreOffice(pptxBytes);
            log.info("[PREVIEW] PDF convertido: {} bytes ({} ms)", pdfBytes.length, System.currentTimeMillis() - t0);

            byte[] pdfConMarca = agregarMarcaAgua(pdfBytes);
            log.info("[PREVIEW] Marca de agua añadida: {} bytes", pdfConMarca.length);

            String nombreBase = estudio.getArchivoNombre() != null
                    ? estudio.getArchivoNombre().replaceAll("\\.[^.]+$", "")
                    : "preview_" + estudioId;
            String nombrePdf = "preview_" + nombreBase + ".pdf";

            String previewClave = s3StorageService.uploadFile(pdfConMarca, nombrePdf, "application/pdf", "estudios/preview");
            log.info("[PREVIEW] PDF subido a S3: {}", previewClave);

            estudio.setArchivoPreviewUrl(previewClave);
            estudioRepository.save(estudio);
            log.info("[PREVIEW] Preview guardado para estudio id={}. Total: {} ms", estudioId, System.currentTimeMillis() - t0);

        } catch (Exception e) {
            log.error("[PREVIEW] Error generando preview para estudio id={}: {}", estudioId, e.getMessage(), e);
        }
    }

    private byte[] convertirConLibreOffice(byte[] pptxBytes) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("lo_conv_");
        Path inputFile = tempDir.resolve("presentacion.pptx");
        Path outputFile = tempDir.resolve("presentacion.pdf");

        try {
            Files.write(inputFile, pptxBytes);

            ProcessBuilder pb = new ProcessBuilder(
                    libreOfficePath,
                    "--headless",
                    "--norestore",
                    "--convert-to", "pdf",
                    "--outdir", tempDir.toAbsolutePath().toString(),
                    inputFile.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(libreOfficeTimeout, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("LibreOffice excedió el tiempo límite de " + libreOfficeTimeout + " s");
            }

            if (!Files.exists(outputFile)) {
                throw new RuntimeException("LibreOffice no generó el PDF. Código=" + process.exitValue() + " Salida=" + output);
            }

            return Files.readAllBytes(outputFile);
        } finally {
            Files.deleteIfExists(inputFile);
            Files.deleteIfExists(outputFile);
            try { Files.deleteIfExists(tempDir); } catch (Exception ignored) {}
        }
    }

    private byte[] agregarMarcaAgua(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            for (PDPage page : doc.getPages()) {
                float w = page.getMediaBox().getWidth();
                float h = page.getMediaBox().getHeight();

                PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                gs.setNonStrokingAlphaConstant(0.10f);
                gs.setAlphaSourceFlag(true);

                try (PDPageContentStream cs = new PDPageContentStream(
                        doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.setGraphicsStateParameters(gs);
                    cs.beginText();
                    cs.setFont(font, 72);
                    cs.setNonStrokingColor(0.0f, 0.427f, 0.216f);
                    // Centrar texto rotado 45° en la página
                    cs.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(45), w * 0.15f, h * 0.25f));
                    cs.showText("RECICLAME");
                    cs.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}

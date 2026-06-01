package com.example.proyectoreciclame.Service;

import com.aspose.slides.IAutoShape;
import com.aspose.slides.IShape;
import com.aspose.slides.ISlide;
import com.aspose.slides.Presentation;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Extrae texto plano de documentos PDF y PPTX para enviarlo a la IA.
 */
@Service
public class TextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TextExtractionService.class);

    /**
     * Extrae texto de un PDF.
     * PDFBox 3.x usa Loader.loadPDF() en lugar de PDDocument.load()
     */
    public String extractTextFromPdf(byte[] pdfData) {
        try (PDDocument doc = Loader.loadPDF(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            log.info("[TEXT] PDF extraído: {} caracteres", text.length());
            return text;
        } catch (IOException e) {
            log.error("[TEXT] Error extrayendo texto de PDF: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Extrae texto de un PPTX.
     * Aspose.Slides IShapeCollection usa get_Item(i), no stream().
     */
    public String extractTextFromPptx(byte[] pptxData) {
        Presentation pres = null;
        try {
            pres = new Presentation(new java.io.ByteArrayInputStream(pptxData));
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < pres.getSlides().size(); i++) {
                ISlide slide = pres.getSlides().get_Item(i);
                for (int j = 0; j < slide.getShapes().size(); j++) {
                    IShape shape = slide.getShapes().get_Item(j);
                    if (shape instanceof IAutoShape) {
                        String text = ((IAutoShape) shape).getTextFrame().getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text).append(" ");
                        }
                    }
                }
            }

            String result = sb.toString().trim();
            log.info("[TEXT] PPTX extraído: {} caracteres", result.length());
            return result;
        } catch (Exception e) {
            log.error("[TEXT] Error extrayendo texto de PPTX: {}", e.getMessage());
            return "";
        } finally {
            if (pres != null) pres.dispose();
        }
    }
}

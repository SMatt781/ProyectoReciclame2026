package com.example.proyectoreciclame.Service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Extrae texto plano de documentos PDF y PPTX para enviarlo a la IA.
 */
@Service
public class TextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TextExtractionService.class);

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
     * Extrae texto de un PPTX usando Apache POI (sin límites de evaluación).
     * Recorre todas las diapositivas y desciende en grupos de formas y tablas
     * para no perder texto anidado.
     */
    public String extractTextFromPptx(byte[] pptxData) {
        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(pptxData))) {
            StringBuilder sb = new StringBuilder();
            for (XSLFSlide slide : ppt.getSlides()) {
                extraerDeShapes(slide.getShapes(), sb);
                sb.append("\n");
            }
            String result = sb.toString().trim();
            log.info("[TEXT] PPTX extraído: {} caracteres de {} diapositivas",
                    result.length(), ppt.getSlides().size());
            return result;
        } catch (Exception e) {
            log.error("[TEXT] Error extrayendo texto de PPTX: {}", e.getMessage());
            return "";
        }
    }

    /** Extrae texto recursivamente de una lista de formas (incluye grupos y tablas). */
    private void extraerDeShapes(List<XSLFShape> shapes, StringBuilder sb) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFGroupShape) {
                extraerDeShapes(((XSLFGroupShape) shape).getShapes(), sb);
            } else if (shape instanceof XSLFTable) {
                for (XSLFTableRow row : ((XSLFTable) shape).getRows()) {
                    for (XSLFTableCell cell : row.getCells()) {
                        String t = cell.getText();
                        if (t != null && !t.isBlank()) sb.append(t).append(" ");
                    }
                }
            } else if (shape instanceof XSLFTextShape) {
                String t = ((XSLFTextShape) shape).getText();
                if (t != null && !t.isBlank()) sb.append(t).append(" ");
            }
        }
    }
}

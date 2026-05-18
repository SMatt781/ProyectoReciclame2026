package com.example.proyectoreciclame.Service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class DocumentConverterService {

    /**
     * Converts PPTX to PDF using Apache POI XSLF + PDFBox.
     * Each slide is rendered to a high-DPI image and embedded as a PDF page.
     * No evaluation watermark.
     */
    public byte[] convertPptxToPdf(byte[] pptxData) {
        try (XMLSlideShow pptx = new XMLSlideShow(new ByteArrayInputStream(pptxData));
             PDDocument doc = new PDDocument()) {

            Dimension pageSize = pptx.getPageSize();
            List<XSLFSlide> slides = pptx.getSlides();

            for (XSLFSlide slide : slides) {
                // Render at 2× for sharpness
                int w = pageSize.width * 2;
                int h = pageSize.height * 2;
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,       RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,      RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.scale(2.0, 2.0);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, pageSize.width, pageSize.height);
                slide.draw(g);
                g.dispose();

                PDPage page = new PDPage(new PDRectangle(pageSize.width, pageSize.height));
                doc.addPage(page);
                PDImageXObject pdImage = JPEGFactory.createFromImage(doc, img, 0.92f);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImage, 0, 0, pageSize.width, pageSize.height);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al convertir PPTX a PDF: " + e.getMessage(), e);
        }
    }

    public byte[] convertXlsxToPdf(byte[] xlsxData) {
        try {
            com.aspose.cells.Workbook workbook = new com.aspose.cells.Workbook(new ByteArrayInputStream(xlsxData));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.save(out, com.aspose.cells.SaveFormat.PDF);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al convertir XLSX a PDF: " + e.getMessage(), e);
        }
    }
}

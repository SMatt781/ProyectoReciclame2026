package com.example.proyectoreciclame.Service;

import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
public class DocumentConverterService {

    public byte[] convertPptxToPdf(byte[] pptxData) {
        Presentation pres = null;
        try {
            pres = new Presentation(new ByteArrayInputStream(pptxData));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pres.save(out, SaveFormat.Pdf);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al convertir PPTX a PDF: " + e.getMessage(), e);
        } finally {
            if (pres != null) pres.dispose();
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

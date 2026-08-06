package com.medprep.service;

import com.medprep.exception.MedPrepException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Extracción de texto de PDFs de exámenes y almacenamiento (best-effort) del
 * archivo original en Supabase Storage bajo el prefijo {@code exams/}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private final StorageService storageService;

    /** Extrae el texto del PDF. Lanza badRequest si está vacío (PDF escaneado/imagen). */
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw MedPrepException.badRequest("El archivo está vacío.");
        }
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(doc);
            if (text == null || text.trim().length() < 30) {
                throw MedPrepException.badRequest(
                        "No se pudo extraer texto del PDF. Parece un PDF escaneado (imagen). " +
                        "Sube un PDF con texto seleccionable.");
            }
            return text;
        } catch (MedPrepException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error leyendo PDF: {}", e.getMessage());
            throw MedPrepException.badRequest("No se pudo leer el PDF: " + e.getMessage());
        }
    }

    /** Sube el PDF original a Storage para dejar registro. No falla si el bucket no existe. */
    public void storeBestEffort(MultipartFile file) {
        try {
            String safeName = (file.getOriginalFilename() == null ? "examen.pdf" : file.getOriginalFilename())
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            String path = "exams/" + UUID.randomUUID() + "_" + safeName;
            storageService.upload(path, file.getBytes(), "application/pdf");
            log.info("Examen almacenado en Storage: {}", path);
        } catch (Exception e) {
            log.warn("No se pudo almacenar el PDF del examen (se continúa con la extracción): {}", e.getMessage());
        }
    }
}

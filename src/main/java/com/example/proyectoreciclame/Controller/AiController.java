package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.SessionUserDto;
import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.Service.AiService;
import com.example.proyectoreciclame.Service.AuthenticatedUserService;
import com.example.proyectoreciclame.Service.S3StorageService;
import com.example.proyectoreciclame.Service.TextExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    @Autowired private AiService                aiService;
    @Autowired private EstudioRepository        estudioRepository;
    @Autowired private NormativaRepository      normativaRepository;
    @Autowired private S3StorageService         s3Service;
    @Autowired private TextExtractionService    textExtractor;
    @Autowired private AuthenticatedUserService authUserService;

    /**
     * GET /ai/estudio/{id}/resumen
     * Genera o devuelve desde caché el resumen ejecutivo de un estudio.
     * Solo accesible por SOCIO (configurado en SecurityConfig).
     */
    @GetMapping("/estudio/{id}/resumen")
    public ResponseEntity<?> generarResumen(@PathVariable Long id) {

        // 1. Obtener usuario autenticado
        SessionUserDto sessionUser = authUserService.obtenerUsuarioSesion();
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Sesión expirada"));
        }
        Long idUsuario = sessionUser.getIdUsuario();

        // 2. Verificar rate limit
        long restantes = aiService.resumenesRestantes(idUsuario);
        if (restantes <= 0) {
            return ResponseEntity.status(429).body(Map.of(
                    "error",   "Límite diario alcanzado",
                    "mensaje", "Has alcanzado el límite de 5 resúmenes por día. Se renueva a medianoche.",
                    "restantes", 0
            ));
        }

        // 3. Obtener el estudio
        Optional<Estudio> estudioOpt = estudioRepository.findById(id);
        if (estudioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Estudio estudio = estudioOpt.get();

        // 4. Descargar y extraer texto del documento
        log.info("[AI-CTRL] Extrayendo texto del estudio {}", id);
        String textoDocumento;
        try {
            String clave = estudio.getArchivoUrl()
                    .replace("/uploads/estudios/", "estudios/");
            String presignedUrl = s3Service.generatePresignedUrl(clave, 300);
            byte[] fileData;
            try (InputStream is = new URL(presignedUrl).openStream()) {
                fileData = is.readAllBytes();
            }

            String formato = estudio.getFormato().name();
            textoDocumento = switch (formato) {
                case "PDF"  -> textExtractor.extractTextFromPdf(fileData);
                case "PPTX" -> textExtractor.extractTextFromPptx(fileData);
                default     -> "";
            };

            if (textoDocumento.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No se pudo extraer texto del documento"));
            }
        } catch (Exception e) {
            log.error("[AI-CTRL] Error descargando documento {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Error al acceder al documento: " + e.getMessage()));
        }

        // 5. Generar resumen con IA
        log.info("[AI-CTRL] Generando resumen para estudio {} usuario {}", id, idUsuario);
        try {
            String resumen = aiService.generarResumenEstudio(
                    id, idUsuario, textoDocumento, estudio.getTitulo());

            if (resumen == null) {
                return ResponseEntity.status(429).body(Map.of(
                        "error", "Límite diario alcanzado",
                        "restantes", 0
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "resumen",   resumen,
                    "restantes", aiService.resumenesRestantes(idUsuario)
            ));
        } catch (Exception e) {
            log.error("[AI-CTRL] Error generando resumen: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Error al generar el resumen: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NORMATIVAS
    // ═══════════════════════════════════════════════════════════════════════════

    /** GET /ai/normativa/{id}/resumen */
    @GetMapping("/normativa/{id}/resumen")
    public ResponseEntity<?> generarResumenNormativa(@PathVariable Long id) {
        SessionUserDto sessionUser = authUserService.obtenerUsuarioSesion();
        if (sessionUser == null) return ResponseEntity.status(401).body(Map.of("error", "Sesión expirada"));
        Long idUsuario = sessionUser.getIdUsuario();

        long restantes = aiService.resumenesRestantes(idUsuario);
        if (restantes <= 0) return ResponseEntity.status(429).body(Map.of(
                "error", "Límite diario alcanzado",
                "mensaje", "Has alcanzado el límite de 5 resúmenes por día. Se renueva a medianoche.",
                "restantes", 0));

        Optional<Normativa> normOpt = normativaRepository.findById(id);
        if (normOpt.isEmpty()) return ResponseEntity.notFound().build();
        Normativa normativa = normOpt.get();

        if (normativa.getArchivoUrl() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Esta normativa no tiene archivo adjunto"));
        }

        log.info("[AI-CTRL] Extrayendo texto de normativa {}", id);
        String textoDocumento;
        try {
            String clave = normativa.getArchivoUrl().replace("/uploads/normativas/", "normativas/");
            String presignedUrl = s3Service.generatePresignedUrl(clave, 300);
            byte[] fileData;
            try (java.io.InputStream is = new java.net.URL(presignedUrl).openStream()) {
                fileData = is.readAllBytes();
            }
            // Normativas son siempre PDF
            textoDocumento = textExtractor.extractTextFromPdf(fileData);
            if (textoDocumento.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No se pudo extraer texto del documento"));
            }
        } catch (Exception e) {
            log.error("[AI-CTRL] Error descargando normativa {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al acceder al documento: " + e.getMessage()));
        }

        try {
            String resumen = aiService.generarResumenNormativa(id, idUsuario, textoDocumento, normativa.getTitulo());
            if (resumen == null) return ResponseEntity.status(429).body(Map.of("error", "Límite diario alcanzado", "restantes", 0));
            return ResponseEntity.ok(Map.of("resumen", resumen, "restantes", aiService.resumenesRestantes(idUsuario)));
        } catch (Exception e) {
            log.error("[AI-CTRL] Error generando resumen normativa: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al generar el resumen: " + e.getMessage()));
        }
    }

    /** GET /ai/normativa/{id}/resumen/restantes */
    @GetMapping("/normativa/{id}/resumen/restantes")
    public ResponseEntity<?> consultarRestantesNormativa() {
        SessionUserDto sessionUser = authUserService.obtenerUsuarioSesion();
        if (sessionUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("restantes", aiService.resumenesRestantes(sessionUser.getIdUsuario())));
    }

    /** GET /ai/diagnostico/modelos — lista modelos disponibles para la API key (temporal) */
    @GetMapping("/diagnostico/modelos")
    public ResponseEntity<?> listarModelos() {
        try {
            String resultado = aiService.listarModelosGemini();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * GET /ai/estudio/{id}/resumen/restantes
     * Cuántos resúmenes le quedan al socio hoy.
     */
    @GetMapping("/estudio/{id}/resumen/restantes")
    public ResponseEntity<?> consultarRestantes() {
        SessionUserDto sessionUser = authUserService.obtenerUsuarioSesion();
        if (sessionUser == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(Map.of(
                "restantes", aiService.resumenesRestantes(sessionUser.getIdUsuario())
        ));
    }
}

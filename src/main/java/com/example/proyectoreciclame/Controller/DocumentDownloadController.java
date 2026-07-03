package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Entity.RegistroDescarga;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.Repository.RegistroDescargaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.DocumentConverterService;
import com.example.proyectoreciclame.Service.NotificacionWebSocketService;
import com.example.proyectoreciclame.Service.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para manejar descargas de documentos desde S3
 * Genera URLs pre-firmadas temporales según el rol del usuario
 */
@Controller
@RequestMapping("/documentos")
public class DocumentDownloadController {

    private static final Logger log = LoggerFactory.getLogger(DocumentDownloadController.class);

    @Autowired
    private EstudioRepository estudioRepository;

    @Autowired
    private NormativaRepository normativaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private S3StorageService s3StorageService;

    @Autowired
    private DocumentConverterService documentConverterService;

    @Autowired
    private RegistroDescargaRepository registroDescargaRepository;

    @Autowired
    private NotificacionWebSocketService webSocketService;

    /**
     * GET /documentos/estudio/{id}/stream
     * Descarga el archivo desde S3 y lo sirve inline con Content-Type correcto.
     * Evita exponer presigned URLs al cliente y elimina problemas de CORS.
     */
    @GetMapping("/estudio/{id}/stream")
    public void streamEstudio(@PathVariable Long id, HttpServletResponse response,
                              Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Estudio estudio = estudioRepository.findById(id).orElse(null);
        if (estudio == null || estudio.getArchivoUrl() == null || estudio.getArchivoUrl().isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            log.info("[STREAM] id={} formato={}", id, estudio.getFormato());

            byte[] pdfData;
            String filename;

            // ── Fast path: PDF pre-generado por PptxPreviewService ──────────
            if (estudio.getFormato() == Estudio.FormatoEstudio.PPTX
                    && estudio.getArchivoPreviewUrl() != null
                    && !estudio.getArchivoPreviewUrl().isBlank()) {
                log.info("[STREAM] Sirviendo preview pre-generado: {}", estudio.getArchivoPreviewUrl());
                pdfData = s3StorageService.downloadFile(estudio.getArchivoPreviewUrl());
                String base = estudio.getArchivoNombre() != null
                        ? estudio.getArchivoNombre().replaceAll("\\.[^.]+$", "")
                        : "presentacion";
                filename = base + ".pdf";
                log.info("[STREAM] Preview OK: {} bytes", pdfData.length);

            // ── Slow path: descarga directa via SDK y conversión on-demand ─
            } else {
                String clave = estudio.getArchivoUrl().replace("/uploads/estudios/", "estudios/");
                log.info("[STREAM] Preview no disponible — descargando on-demand via SDK: {}", clave);
                byte[] fileData = s3StorageService.downloadFile(clave);
                log.info("[STREAM] Descarga OK: {} bytes", fileData.length);

                if (estudio.getFormato() == Estudio.FormatoEstudio.PPTX) {
                    log.info("[STREAM] Convirtiendo PPTX a PDF con Aspose (fallback)...");
                    pdfData = documentConverterService.convertPptxToPdf(fileData);
                    log.info("[STREAM] Conversión OK: {} bytes PDF", pdfData.length);
                    String base = estudio.getArchivoNombre() != null
                            ? estudio.getArchivoNombre().replaceAll("\\.[^.]+$", "")
                            : "presentacion";
                    filename = base + ".pdf";
                } else {
                    pdfData = fileData;
                    filename = estudio.getArchivoNombre() != null ? estudio.getArchivoNombre() : "documento.pdf";
                }
            }

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLength(pdfData.length);
            response.getOutputStream().write(pdfData);
            response.getOutputStream().flush();
            log.info("[STREAM] Enviado OK");
        } catch (Exception e) {
            if (e.getClass().getName().contains("ClientAbortException")) {
                log.debug("[STREAM] El cliente abortó la conexión antes de terminar la descarga.");
            } else {
                log.error("[STREAM] ERROR: {}", e.getMessage(), e);
                if (!response.isCommitted()) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Error al cargar el documento: " + e.getMessage());
                }
            }
        }
    }

    /**
     * GET /documentos/estudio/{id}/presigned-url
     * Genera una URL pre-firmada temporal para descargar un estudio
     * La duración depende del rol del usuario
     */
    @GetMapping("/estudio/{id}/presigned-url")
    public ResponseEntity<?> getEstudioPresignedUrl(@PathVariable Long id) {
        try {
            // Obtener usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByCorreoAndEliminadoEnIsNull(email).orElse(null);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            // Obtener estudio
            Estudio estudio = estudioRepository.findById(id).orElse(null);
            if (estudio == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Estudio no encontrado"));
            }

            // Si no tiene archivo, retornar error
            if (estudio.getArchivoNombre() == null || estudio.getArchivoNombre().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "El estudio no tiene archivo adjunto"));
            }

            // Generar URL pre-firmada según el rol
            String clave = estudio.getArchivoUrl().replace("/uploads/estudios/", "estudios/");
            String rolNombre = usuario.getRol().getNombre();
            String presignedUrl = s3StorageService.generatePresignedUrlByRole(clave, rolNombre);

            // Retornar respuesta con la URL
            Map<String, String> response = new HashMap<>();
            response.put("url", presignedUrl);
            response.put("nombreArchivo", estudio.getArchivoNombre());
            response.put("rolUsuario", rolNombre);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al generar URL: " + e.getMessage()));
        }
    }

    /**
     * GET /documentos/estudio/{id}/download
     * Redirige al navegador a una presigned URL con Content-Disposition: attachment.
     * El navegador muestra la barra de descarga nativa inmediatamente.
     */
    @GetMapping("/estudio/{id}/download")
    public void downloadEstudio(@PathVariable Long id, HttpServletResponse response,
                                Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Estudio estudio = estudioRepository.findById(id).orElse(null);
        if (estudio == null || estudio.getArchivoUrl() == null || estudio.getArchivoUrl().isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByCorreoAndEliminadoEnIsNull(email).orElse(null);
        int duration = 300;
        if (usuario != null) {
            String rol = usuario.getRol().getNombre().toUpperCase();
            if (rol.equals("ADMIN") || rol.equals("SUPERADMIN")) duration = 3600;
            else if (rol.equals("SOCIO")) duration = 1800;
            else if (rol.equals("VISUALIZADOR")) duration = 900;
        }

        String clave = estudio.getArchivoUrl().replace("/uploads/estudios/", "estudios/");
        String filename = estudio.getArchivoNombre() != null ? estudio.getArchivoNombre() : "documento";
        String downloadUrl = s3StorageService.generatePresignedDownloadUrl(clave, filename, duration);

        if (usuario != null) {
            RegistroDescarga reg = new RegistroDescarga();
            reg.setUsuario(usuario);
            reg.setTipoDocumento("ESTUDIO");
            reg.setIdDocumento(id);
            reg.setNombreDocumento(filename);
            reg.setUrlDescargada("/documentos/estudio/" + id + "/download");
            reg.setFechaDescarga(LocalDateTime.now());
            registroDescargaRepository.save(reg);
            webSocketService.enviarTopico("/topic/admin/descargas", Map.of("event", "download"));
        }

        response.sendRedirect(downloadUrl);
    }

    /**
     * GET /documentos/normativa/{id}/download
     * Redirige al navegador a una presigned URL con Content-Disposition: attachment.
     */
    @GetMapping("/normativa/{id}/download")
    public void downloadNormativa(@PathVariable Long id, HttpServletResponse response,
                                  Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Normativa normativa = normativaRepository.findById(id).orElse(null);
        if (normativa == null || normativa.getArchivoUrl() == null || normativa.getArchivoUrl().isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByCorreoAndEliminadoEnIsNull(email).orElse(null);
        int duration = 300;
        if (usuario != null) {
            String rol = usuario.getRol().getNombre().toUpperCase();
            if (rol.equals("ADMIN") || rol.equals("SUPERADMIN")) duration = 3600;
            else if (rol.equals("SOCIO")) duration = 1800;
            else if (rol.equals("VISUALIZADOR")) duration = 900;
        }

        String clave = normativa.getArchivoUrl().replace("/uploads/normativas/", "normativas/");
        String filename = normativa.getArchivoNombre() != null ? normativa.getArchivoNombre() : "normativa";
        String downloadUrl = s3StorageService.generatePresignedDownloadUrl(clave, filename, duration);

        if (usuario != null) {
            RegistroDescarga reg = new RegistroDescarga();
            reg.setUsuario(usuario);
            reg.setTipoDocumento("NORMATIVA");
            reg.setIdDocumento(id);
            reg.setNombreDocumento(filename);
            reg.setUrlDescargada("/documentos/normativa/" + id + "/download");
            reg.setFechaDescarga(LocalDateTime.now());
            registroDescargaRepository.save(reg);
            webSocketService.enviarTopico("/topic/admin/descargas", Map.of("event", "download"));
        }

        response.sendRedirect(downloadUrl);
    }

    /**
     * GET /documentos/estudio/{id}/view-pdf
     * Descarga el PPTX desde S3, lo convierte a PDF con DocumentConverterService y lo sirve inline.
     * Evita depender de Microsoft Office Online (que no puede acceder a URLs presignadas privadas).
     */
    @GetMapping("/estudio/{id}/view-pdf")
    public void viewEstudioAsPdf(@PathVariable Long id, HttpServletResponse response,
                                 Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Estudio estudio = estudioRepository.findById(id).orElse(null);
        if (estudio == null || estudio.getArchivoUrl() == null || estudio.getArchivoUrl().isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            String clave = estudio.getArchivoUrl().replace("/uploads/estudios/", "estudios/");
            byte[] fileData = s3StorageService.downloadFile(clave);

            byte[] pdfData;
            String filename;
            if (estudio.getFormato() == Estudio.FormatoEstudio.PPTX) {
                pdfData = documentConverterService.convertPptxToPdf(fileData);
                String base = estudio.getArchivoNombre() != null
                        ? estudio.getArchivoNombre().replaceAll("\\.[^.]+$", "")
                        : "presentacion";
                filename = base + ".pdf";
            } else {
                pdfData = fileData;
                filename = estudio.getArchivoNombre() != null ? estudio.getArchivoNombre() : "documento.pdf";
            }

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLength(pdfData.length);
            response.getOutputStream().write(pdfData);
            response.getOutputStream().flush();
        } catch (Exception e) {
            if (e.getClass().getName().contains("ClientAbortException")) {
                log.debug("[STREAM] El cliente abortó la conexión (view-pdf).");
            } else {
                log.error("[STREAM] ERROR: {}", e.getMessage(), e);
                if (!response.isCommitted()) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Error al cargar el documento: " + e.getMessage());
                }
            }
        }
    }

    /**
     * GET /documentos/normativa/{id}/presigned-url
     * Genera una URL pre-firmada temporal para descargar una normativa
     * La duración depende del rol del usuario
     */
    @GetMapping("/normativa/{id}/presigned-url")
    public ResponseEntity<?> getNormativaPresignedUrl(@PathVariable Long id) {
        try {
            // Obtener usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByCorreoAndEliminadoEnIsNull(email).orElse(null);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            // Obtener normativa
            Normativa normativa = normativaRepository.findById(id).orElse(null);
            if (normativa == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Normativa no encontrada"));
            }

            // Si no tiene archivo, retornar error
            if (normativa.getArchivoNombre() == null || normativa.getArchivoNombre().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "La normativa no tiene archivo adjunto"));
            }

            // Generar URL pre-firmada según el rol
            String clave = normativa.getArchivoUrl().replace("/uploads/normativas/", "normativas/");
            String rolNombre = usuario.getRol().getNombre();
            String presignedUrl = s3StorageService.generatePresignedUrlByRole(clave, rolNombre);

            // Retornar respuesta con la URL
            Map<String, String> response = new HashMap<>();
            response.put("url", presignedUrl);
            response.put("nombreArchivo", normativa.getArchivoNombre());
            response.put("rolUsuario", rolNombre);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al generar URL: " + e.getMessage()));
        }
    }
}

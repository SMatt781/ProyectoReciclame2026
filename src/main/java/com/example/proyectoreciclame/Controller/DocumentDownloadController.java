package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.S3StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para manejar descargas de documentos desde S3
 * Genera URLs pre-firmadas temporales según el rol del usuario
 */
@Controller
@RequestMapping("/documentos")
public class DocumentDownloadController {

    @Autowired
    private EstudioRepository estudioRepository;

    @Autowired
    private NormativaRepository normativaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private S3StorageService s3StorageService;

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

package com.example.proyectoreciclame.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Servicio para gestionar fotos de perfil de usuarios
 * Soporta dos tipos de almacenamiento:
 * - LOCAL: guarda en el disco local (development)
 * - S3: guarda en AWS S3 (production)
 */
@Service
public class PerfilFotoService {

    @Value("${storage.type:local}")
    private String storageType;

    @Value("${upload.dir:uploads/}")
    private String uploadDir;

    @Value("${upload.avatar-dir:uploads/avatares/}")
    private String avatarDir;

    @Value("${upload.max-file-size:5242880}") // 5MB por defecto
    private long maxFileSize;

    @Value("${upload.allowed-formats:jpg,jpeg,png,gif}")
    private String allowedFormats;

    private final UsuarioRepository usuarioRepository;
    private final S3StorageService s3StorageService;

    // MIME types permitidos
    private static final Set<String> MIME_TYPES_PERMITIDOS = new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    ));

    public PerfilFotoService(UsuarioRepository usuarioRepository, S3StorageService s3StorageService) {
        this.usuarioRepository = usuarioRepository;
        this.s3StorageService = s3StorageService;
    }

    /**
     * Guarda la foto de perfil de un usuario
     * Automáticamente elige entre LOCAL o S3 según configuración
     * @param usuarioId ID del usuario
     * @param archivo   Archivo de imagen a guardar
     * @return URL relativa de la imagen guardada
     */
    public String guardarFoto(Long usuarioId, MultipartFile archivo) throws IOException {
        validarImagen(archivo);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Eliminar foto anterior si existe
        if (usuario.getUrlAvatar() != null && !usuario.getUrlAvatar().isEmpty()) {
            eliminarFotoAnterior(usuario.getUrlAvatar());
        }

        String urlAvatar;
        if ("s3".equalsIgnoreCase(storageType)) {
            urlAvatar = guardarEnS3(usuarioId, archivo);
        } else {
            urlAvatar = guardarEnLocal(usuarioId, archivo);
        }

        usuario.setUrlAvatar(urlAvatar);
        usuario.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return urlAvatar;
    }

    /**
     * Guarda la foto en el sistema de archivos local
     */
    private String guardarEnLocal(Long usuarioId, MultipartFile archivo) throws IOException {
        Path directorioAvatares = Paths.get(avatarDir);
        Files.createDirectories(directorioAvatares);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String extension = obtenerExtension(archivo.getOriginalFilename());
        String nombreArchivo = usuarioId + "_" + timestamp + "." + extension;

        Path rutaArchivo = directorioAvatares.resolve(nombreArchivo);
        Files.write(rutaArchivo, archivo.getBytes());

        return "/uploads/avatares/" + nombreArchivo;
    }

    /**
     * Guarda la foto en AWS S3
     */
    private String guardarEnS3(Long usuarioId, MultipartFile archivo) throws IOException {
        String carpeta = "avatares";
        String claveS3 = s3StorageService.uploadFile(archivo, carpeta);
        // Retorna la clave S3 que se usará para generar pre-signed URLs
        return claveS3;
    }

    /**
     * Elimina la foto anterior de un usuario
     * @param urlFoto URL o clave S3 de la foto a eliminar
     */
    public void eliminarFotoAnterior(String urlFoto) {
        try {
            if (urlFoto == null || urlFoto.isEmpty()) {
                return;
            }

            if ("s3".equalsIgnoreCase(storageType)) {
                // urlFoto es la clave S3
                s3StorageService.deleteFile(urlFoto);
            } else {
                // urlFoto es la ruta local
                String nombreArchivo = urlFoto.substring(urlFoto.lastIndexOf("/") + 1);
                Path rutaArchivo = Paths.get(avatarDir).resolve(nombreArchivo);

                if (Files.exists(rutaArchivo)) {
                    Files.delete(rutaArchivo);
                }
            }
        } catch (Exception e) {
            System.err.println("Error eliminando foto anterior: " + e.getMessage());
        }
    }

    /**
     * Valida que el archivo sea una imagen válida
     */
    public void validarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }

        if (archivo.getSize() > maxFileSize) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido de 5MB");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !MIME_TYPES_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException("El archivo debe ser una imagen válida (JPG, PNG, GIF, WebP)");
        }

        String extension = obtenerExtension(archivo.getOriginalFilename()).toLowerCase();
        Set<String> extensionesPermitidas = new HashSet<>(Arrays.asList(
                "jpg", "jpeg", "png", "gif", "webp"
        ));

        if (!extensionesPermitidas.contains(extension)) {
            throw new IllegalArgumentException("Extensión de archivo no permitida");
        }
    }

    /**
     * Elimina completamente la foto de perfil de un usuario
     */
    public void eliminarFoto(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (usuario.getUrlAvatar() != null && !usuario.getUrlAvatar().isEmpty()) {
            eliminarFotoAnterior(usuario.getUrlAvatar());
        }

        usuario.setUrlAvatar(null);
        usuario.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    /**
     * Obtiene la URL de la foto actual del usuario
     */
    public String obtenerFotoUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(Usuario::getUrlAvatar)
                .orElse(null);
    }

    /**
     * Extrae la extensión de un nombre de archivo
     */
    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1);
    }
}

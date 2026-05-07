package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Config.AwsS3Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;

/**
 * Servicio para manejar carga y descarga de archivos desde AWS S3
 * Genera URLs temporales (pre-signed URLs) con diferentes duraciones según el rol del usuario
 */
@Service
public class S3StorageService {

    @Autowired
    private AwsS3Config s3Config;

    /**
     * Crea un cliente S3 con credenciales desde application.properties o variables de entorno
     */
    private S3Client getS3Client() {
        // Primero intenta obtener credenciales desde las variables de entorno (AWS SDK las busca automáticamente)
        // Si no las encuentra, usa las del archivo ~/.aws/credentials
        // Si tampoco las encuentra, intenta usar las de application.properties

        if (s3Config.getSessionToken() != null && !s3Config.getSessionToken().isEmpty()) {
            // Credenciales temporales (con session token)
            AwsSessionCredentials credentials = AwsSessionCredentials.create(
                    s3Config.getAccessKey(),
                    s3Config.getSecretKey(),
                    s3Config.getSessionToken()
            );

            return S3Client.builder()
                    .region(Region.of(s3Config.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        } else if (s3Config.getAccessKey() != null && s3Config.getSecretKey() != null) {
            // Credenciales básicas
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                    s3Config.getAccessKey(),
                    s3Config.getSecretKey()
            );

            return S3Client.builder()
                    .region(Region.of(s3Config.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .build();
        } else {
            // Usa las credenciales del archivo ~/.aws/credentials o variables de entorno
            return S3Client.builder()
                    .region(Region.of(s3Config.getRegion()))
                    .build();
        }
    }

    /**
     * Crea un presigner de S3 con credenciales
     */
    private S3Presigner getS3Presigner() {
        if (s3Config.getSessionToken() != null && !s3Config.getSessionToken().isEmpty()) {
            AwsSessionCredentials credentials = AwsSessionCredentials.create(
                    s3Config.getAccessKey(),
                    s3Config.getSecretKey(),
                    s3Config.getSessionToken()
            );

            return S3Presigner.builder()
                    .region(Region.of(s3Config.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        } else if (s3Config.getAccessKey() != null && s3Config.getSecretKey() != null) {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                    s3Config.getAccessKey(),
                    s3Config.getSecretKey()
            );

            return S3Presigner.builder()
                    .region(Region.of(s3Config.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .build();
        } else {
            return S3Presigner.builder()
                    .region(Region.of(s3Config.getRegion()))
                    .build();
        }
    }

    /**
     * Sube un archivo a S3
     * @param file Archivo a subir
     * @param carpeta Carpeta destino en S3 (ej: "estudios", "normativas")
     * @return URL del archivo en S3
     */
    public String uploadFile(MultipartFile file, String carpeta) {
        try {
            String nombreOriginal = file.getOriginalFilename();
            if (nombreOriginal == null || nombreOriginal.isEmpty()) {
                throw new RuntimeException("Nombre de archivo inválido");
            }

            // Generar nombre único del archivo
            String nombreArchivo = System.currentTimeMillis() + "_" + nombreOriginal;
            String clave = carpeta + "/" + nombreArchivo;

            // Crear cliente S3
            S3Client s3Client = getS3Client();

            // Subir archivo
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(clave)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                    file.getInputStream(),
                    file.getSize()
            ));

            s3Client.close();

            // Retornar la clave (usaremos esta para generar pre-signed URLs después)
            return clave;

        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo a S3: " + e.getMessage(), e);
        }
    }

    /**
     * Genera una URL pre-firmada (temporal) para descargar/ver un archivo
     * La URL expira después del tiempo especificado
     * @param clave Clave del archivo en S3 (ej: "estudios/timestamp_nombre.pdf")
     * @param durationSeconds Duración de validez de la URL en segundos
     * @return URL pre-firmada temporal
     */
    public String generatePresignedUrl(String clave, int durationSeconds) {
        try {
            S3Presigner presigner = getS3Presigner();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(durationSeconds))
                    .getObjectRequest(builder -> builder
                            .bucket(s3Config.getBucketName())
                            .key(clave)
                            .build())
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            presigner.close();

            return presignedUrl;

        } catch (Exception e) {
            throw new RuntimeException("Error al generar URL pre-firmada: " + e.getMessage(), e);
        }
    }

    /**
     * Genera una URL pre-firmada según el rol del usuario
     * ADMIN: 3600 segundos (1 hora)
     * SOCIO: 1800 segundos (30 minutos)
     * VISUALIZADOR: 900 segundos (15 minutos)
     * @param clave Clave del archivo
     * @param rolNombre Nombre del rol del usuario
     * @return URL pre-firmada con duración según el rol
     */
    public String generatePresignedUrlByRole(String clave, String rolNombre) {
        int durationSeconds;

        switch (rolNombre.toUpperCase()) {
            case "ADMIN":
            case "SUPERADMIN":
                durationSeconds = 3600; // 1 hora
                break;
            case "SOCIO":
                durationSeconds = 1800; // 30 minutos
                break;
            case "VISUALIZADOR":
                durationSeconds = 900; // 15 minutos
                break;
            default:
                durationSeconds = 600; // 10 minutos por defecto
        }

        return generatePresignedUrl(clave, durationSeconds);
    }

    /**
     * Elimina un archivo de S3
     * @param clave Clave del archivo a eliminar
     */
    public void deleteFile(String clave) {
        try {
            S3Client s3Client = getS3Client();

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(clave)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            s3Client.close();

        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar archivo de S3: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el nombre del archivo desde la clave S3
     * @param clave Clave completa del archivo (ej: "estudios/timestamp_nombre.pdf")
     * @return Nombre del archivo (ej: "timestamp_nombre.pdf")
     */
    public String getFileName(String clave) {
        if (clave == null || clave.isEmpty()) {
            return "";
        }
        return clave.substring(clave.lastIndexOf("/") + 1);
    }
}

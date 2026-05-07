package com.example.proyectoreciclame.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de propiedades de AWS S3
 * Lee las propiedades desde application.properties con prefijo 'aws.s3'
 */
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class AwsS3Config {

    private String bucketName;
    private String region;
    private String accessKey;
    private String secretKey;
    private String sessionToken;
    private Integer presignedDuration = 900; // 15 minutos por defecto

    // Getters y Setters
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Integer getPresignedDuration() {
        return presignedDuration;
    }

    public void setPresignedDuration(Integer presignedDuration) {
        this.presignedDuration = presignedDuration;
    }
}

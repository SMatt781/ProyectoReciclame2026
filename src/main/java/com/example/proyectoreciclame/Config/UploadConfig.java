package com.example.proyectoreciclame.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

/**
 * Configuración para servir archivos subidos (upload)
 * Mapea las URLs /uploads/** a los archivos del sistema de archivos
 */
@Configuration
public class UploadConfig implements WebMvcConfigurer {

    @Value("${upload.dir:uploads/}")
    private String uploadDir;

    /**
     * Registra un manejador de recursos para servir archivos subidos
     * Mapea /uploads/** a los archivos en el directorio configurado
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(3600); // Cache por 1 hora
    }
}

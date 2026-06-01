package com.example.proyectoreciclame.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_resumen_cache")
@Getter @Setter @NoArgsConstructor
public class AiResumenCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cache")
    private Long idCache;

    /** "ESTUDIO" o "NORMATIVA" */
    @Column(name = "tipo_doc", length = 10, nullable = false)
    private String tipoDoc;

    @Column(name = "id_documento", nullable = false)
    private Long idDocumento;

    @Column(name = "resumen", nullable = false, columnDefinition = "TEXT")
    private String resumen;

    @Column(name = "generado_en", nullable = false)
    private LocalDateTime generadoEn;

    @Column(name = "proveedor", length = 20)
    private String proveedor;

    public AiResumenCache(String tipoDoc, Long idDocumento, String resumen, String proveedor) {
        this.tipoDoc     = tipoDoc;
        this.idDocumento = idDocumento;
        this.resumen     = resumen;
        this.proveedor   = proveedor;
        this.generadoEn  = LocalDateTime.now();
    }
}

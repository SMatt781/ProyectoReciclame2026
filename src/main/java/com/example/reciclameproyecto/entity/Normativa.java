package com.example.reciclameproyecto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "normativas")
@EntityListeners(AuditingEntityListener.class)
public class Normativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_normativa")
    private Long idNormativa;

    @NotBlank
    @Size(max = 400)
    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Size(max = 2000)
    @Column(name = "descripcion")
    private String descripcion;

    @Size(max = 50)
    @Column(name = "codigo")
    private String codigo;

    @NotBlank
    @Size(max = 200)
    @Column(name = "organismo_emisor", nullable = false)
    private String organismoEmisor;

    @NotNull
    @Column(name = "anio", nullable = false)
    private Integer anio;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_norma", nullable = false)
    private TipoNorma tipoNorma;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoNormativa estado;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "acceso", nullable = false)
    private AccesoNormativa acceso;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "alcance", nullable = false)
    private AlcanceNormativa alcance;

    @Size(max = 1000)
    @Column(name = "campo_aplicacion")
    private String campoAplicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "clasificacion")
    private ClasificacionNormativa clasificacion;

    @Size(max = 100)
    @Column(name = "obligatoriedad")
    private String obligatoriedad;

    @Size(max = 255)
    @Column(name = "archivo_nombre")
    private String archivoNombre;

    @Size(max = 500)
    @Column(name = "archivo_url")
    private String archivoUrl;

    @Size(max = 500)
    @Column(name = "enlace_externo")
    private String enlaceExterno;

    @ManyToOne
    @JoinColumn(name = "id_usuario_creador", nullable = false)
    private Usuario usuarioCreador;

    @ManyToOne
    @JoinColumn(name = "id_usuario_editor")
    private Usuario usuarioEditor;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "eliminado_en")
    private LocalDateTime eliminadoEn;

    @ManyToMany
    @JoinTable(
        name = "normativa_categoria",
        joinColumns = @JoinColumn(name = "id_normativa"),
        inverseJoinColumns = @JoinColumn(name = "id_categoria")
    )
    private List<Categoria> categorias;

    public enum TipoNorma {
        LEY_NACIONAL, DECRETO_SUPREMO, REGLAMENTO, ANTEPROYECTO, HOJA_DE_RUTA, DECRETO_LEY, OTRO
    }

    public enum EstadoNormativa {
        VIGENTE, DEROGADA, PUBLICADA, CONSULTA_PUBLICA, BORRADOR_EN_PROCESO
    }

    public enum AccesoNormativa {
        GRATIS, PAGO
    }

    public enum AlcanceNormativa {
        NACIONAL, INTERNACIONAL
    }

    public enum ClasificacionNormativa {
        PRIORIDAD_ALTA, PRIORIDAD_MEDIA, PRIORIDAD_BAJA, REFERENCIA_TECNICA, PLANEAMIENTO, INNOVACION_NORMATIVA, OBLIGATORIA, COMPLEMENTARIA
    }
}

package com.example.reciclameproyecto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "estudios")
@EntityListeners(AuditingEntityListener.class)
public class Estudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudio")
    private Long idEstudio;

    @NotBlank
    @Size(max = 300)
    @Column(name = "titulo", nullable = false)
    private String titulo;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @NotNull
    @Column(name = "anio", nullable = false)
    private Integer anio;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "formato", nullable = false)
    private FormatoEstudio formato;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoEstudio estado = EstadoEstudio.BORRADOR;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acceso", nullable = false)
    private TipoAcceso tipoAcceso;

    @Size(max = 255)
    @Column(name = "archivo_nombre")
    private String archivoNombre;

    @Size(max = 500)
    @Column(name = "archivo_url")
    private String archivoUrl;

    @Column(name = "archivo_tamanio_kb")
    private Integer archivoTamanioKb;

    @Column(name = "fecha_publicacion")
    private LocalDate fechaPublicacion;

    @Column(name = "indice_relevancia")
    private Integer indiceRelevancia;

    @Size(max = 1000)
    @Column(name = "informacion_legal")
    private String informacionLegal;

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
        name = "estudio_categoria",
        joinColumns = @JoinColumn(name = "id_estudio"),
        inverseJoinColumns = @JoinColumn(name = "id_categoria")
    )
    private List<Categoria> categorias;

    public enum FormatoEstudio {
        PDF, PPTX
    }

    public enum EstadoEstudio {
        VIGENTE, BORRADOR, DEROGADO
    }

    public enum TipoAcceso {
        LECTURA, DESCARGA
    }
}

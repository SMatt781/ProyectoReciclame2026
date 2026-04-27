package com.example.proyectoreciclame.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "actividad_sistema")
@EntityListeners(AuditingEntityListener.class)
public class ActividadSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad")
    private Long idActividad;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoActividad tipo;

    @Column(name = "id_referencia")
    private Long idReferencia;

    @NotBlank
    @Size(max = 500)
    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false)
    private AccionActividad accion;

    @CreatedDate
    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    public enum TipoActividad {
        ESTUDIO, NORMATIVA, USUARIO, INDICADOR, PUBLICACION
    }

    public enum AccionActividad {
        AÑADIDO, MODIFICADO, ELIMINADO
    }
}

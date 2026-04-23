package com.example.proyectoreciclame.Entity;

import com.example.proyectoreciclame.Entity.Usuario;
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
@Table(name = "notificaciones")
@EntityListeners(AuditingEntityListener.class)
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @NotBlank
    @Size(max = 200)
    @Column(name = "titulo", nullable = false)
    private String titulo;

    @NotBlank
    @Size(max = 1000)
    @Column(name = "mensaje", nullable = false)
    private String mensaje;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoNotificacion tipo;

    @NotNull
    @Column(name = "leido", nullable = false)
    private Boolean leido = false;

    @Size(max = 500)
    @Column(name = "enlace_referencia")
    private String enlaceReferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entidad")
    private TipoEntidadNotificacion tipoEntidad;

    @Column(name = "id_entidad")
    private Long idEntidad;

    @CreatedDate
    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    public enum TipoNotificacion {
        SISTEMA, ESTUDIO, NORMATIVA, SOLICITUD
    }

    public enum TipoEntidadNotificacion {
        USUARIO, ESTUDIO, NORMATIVA, PUBLICACION, SOLICITUD_REGISTRO, AUDITORIA, CONFIGURACION
    }
}

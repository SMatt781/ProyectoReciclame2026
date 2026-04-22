package com.example.reciclameproyecto.entity;

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
@Table(name = "solicitud_registro")
@EntityListeners(AuditingEntityListener.class)
public class SolicitudRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombres", nullable = false)
    private String nombres;

    @NotBlank
    @Size(max = 100)
    @Column(name = "apellido_paterno", nullable = false)
    private String apellidoPaterno;

    @Size(max = 100)
    @Column(name = "apellido_materno")
    private String apellidoMaterno;

    @NotBlank
    @Size(min = 8, max = 8)
    @Column(name = "dni", nullable = false)
    private String dni;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(name = "correo", nullable = false)
    private String correo;

    @Size(max = 20)
    @Column(name = "telefono")
    private String telefono;

    @Size(min = 11, max = 11)
    @Column(name = "ruc")
    private String ruc;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "rol_solicitado", nullable = false)
    private RolSolicitado rolSolicitado;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Size(max = 1000)
    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @ManyToOne
    @JoinColumn(name = "revisado_por")
    private Usuario revisadoPor;

    @ManyToOne
    @JoinColumn(name = "id_usuario_creado")
    private Usuario usuarioCreado;

    @CreatedDate
    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    public enum RolSolicitado {
        SOCIO, VISUALIZADOR
    }

    public enum EstadoSolicitud {
        PENDIENTE, APROBADO, RECHAZADO
    }
}

package com.example.reciclameproyecto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
@EntityListeners(AuditingEntityListener.class)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

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
    @Column(name = "dni", nullable = false, unique = true)
    private String dni;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Size(max = 20)
    @Column(name = "telefono")
    private String telefono;

    @NotBlank
    @Size(max = 255)
    @Column(name = "contrasena_hash", nullable = false)
    private String contrasenaHash;

    @Size(max = 500)
    @Column(name = "url_avatar")
    private String urlAvatar;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_aprobacion", nullable = false)
    private EstadoAprobacion estadoAprobacion = EstadoAprobacion.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta")
    private EstadoCuenta estadoCuenta;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @CreatedDate
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @LastModifiedDate
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @Column(name = "eliminado_en")
    private LocalDateTime eliminadoEn;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL)
    private UsuarioEmpresa usuarioEmpresa;

    public enum EstadoAprobacion {
        PENDIENTE, APROBADO, RECHAZADO
    }

    public enum EstadoCuenta {
        ACTIVO, BLOQUEADO
    }
}

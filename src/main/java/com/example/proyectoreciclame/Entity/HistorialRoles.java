package com.example.proyectoreciclame.Entity;

import com.example.proyectoreciclame.Entity.Rol;
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
@Table(name = "historial_roles")
@EntityListeners(AuditingEntityListener.class)
public class HistorialRoles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial_rol")
    private Long idHistorialRol;

    @ManyToOne
    @JoinColumn(name = "id_usuario_afectado", nullable = false)
    private Usuario usuarioAfectado;

    @ManyToOne
    @JoinColumn(name = "id_rol_anterior")
    private Rol rolAnterior;

    @ManyToOne
    @JoinColumn(name = "id_rol_nuevo")
    private Rol rolNuevo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private Usuario.EstadoCuenta estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo")
    private Usuario.EstadoCuenta estadoNuevo;

    @ManyToOne
    @JoinColumn(name = "autorizado_por", nullable = false)
    private Usuario autorizadoPor;

    @Size(max = 1000)
    @Column(name = "motivo")
    private String motivo;

    @CreatedDate
    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio;
}

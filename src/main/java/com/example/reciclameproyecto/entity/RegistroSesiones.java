package com.example.reciclameproyecto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "registro_sesiones")
public class RegistroSesiones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Long idSesion;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Size(max = 255)
    @Column(name = "token", unique = true)
    private String token;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Size(max = 45)
    @Column(name = "ip")
    private String ip;

    @Size(max = 500)
    @Column(name = "agente_usuario")
    private String agenteUsuario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoSesion estado = EstadoSesion.VIGENTE;

    public enum EstadoSesion {
        VIGENTE, FINALIZADA, EXPIRADA
    }
}

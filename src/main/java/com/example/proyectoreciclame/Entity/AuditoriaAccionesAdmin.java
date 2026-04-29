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
@Table(name = "auditoria_acciones_admin")
@EntityListeners(AuditingEntityListener.class)
public class AuditoriaAccionesAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @NotBlank
    @Size(max = 30)
    @Column(name = "accion", nullable = false)
    private String accion;

    @NotBlank
    @Size(max = 50)
    @Column(name = "modulo", nullable = false)
    private String modulo;

    @Column(name = "id_entidad")
    private Long idEntidad;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "datos_anteriores", columnDefinition = "JSON")
    private String datosAnteriores;

    @Column(name = "datos_nuevos", columnDefinition = "JSON")
    private String datosNuevos;

    @CreatedDate
    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Size(max = 45)
    @Column(name = "ip")
    private String ip;
}

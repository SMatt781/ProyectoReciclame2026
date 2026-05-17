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
@Table(name = "historial_normativas")
@EntityListeners(AuditingEntityListener.class)
public class HistorialNormativas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    @ManyToOne
    @JoinColumn(name = "id_normativa", nullable = false)
    private Normativa normativa;

    @NotBlank
    @Size(max = 100)
    @Column(name = "campo_modificado", nullable = false)
    private String campoModificado;

    @Size(max = 2000)
    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Size(max = 2000)
    @Column(name = "valor_nuevo")
    private String valorNuevo;

    @ManyToOne
    @JoinColumn(name = "modificado_por", nullable = false)
    private Usuario modificadoPor;

    @NotNull
    @Column(name = "es_rollback", nullable = false)
    private Boolean esRollback = false;

    @ManyToOne
    @JoinColumn(name = "id_version_origen")
    private HistorialNormativas versionOrigen;

    @CreatedDate
    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio;
}

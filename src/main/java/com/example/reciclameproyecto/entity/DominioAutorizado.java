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
@Table(name = "dominio_autorizado")
@EntityListeners(AuditingEntityListener.class)
public class DominioAutorizado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dominio")
    private Integer idDominio;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre_dominio", nullable = false, unique = true)
    private String nombreDominio;

    @Size(max = 500)
    @Column(name = "motivo_autorizacion")
    private String motivoAutorizacion;

    @NotNull
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @ManyToOne
    @JoinColumn(name = "id_usuario_creador", nullable = false)
    private Usuario usuarioCreador;

    @CreatedDate
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}

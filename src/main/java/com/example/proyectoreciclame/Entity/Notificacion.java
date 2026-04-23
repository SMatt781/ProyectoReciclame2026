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

    @ManyToOne(fetch = FetchType.LAZY)
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
    @Column(name = "leido", nullable = false)
    private Boolean leido = false;

    private LocalDateTime fecha;


}

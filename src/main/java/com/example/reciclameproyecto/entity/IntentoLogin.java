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
@Table(name = "intento_login")
@EntityListeners(AuditingEntityListener.class)
public class IntentoLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_intento")
    private Long idIntento;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(name = "correo", nullable = false)
    private String correo;

    @CreatedDate
    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Size(max = 45)
    @Column(name = "ip")
    private String ip;

    @NotNull
    @Column(name = "exitoso", nullable = false)
    private Boolean exitoso = false;
}

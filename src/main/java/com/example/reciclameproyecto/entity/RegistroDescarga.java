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
@Table(name = "registro_descarga")
@EntityListeners(AuditingEntityListener.class)
public class RegistroDescarga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_descarga")
    private Long idDescarga;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    @NotNull
    @Column(name = "id_documento", nullable = false)
    private Long idDocumento;

    @NotBlank
    @Size(max = 400)
    @Column(name = "nombre_documento", nullable = false)
    private String nombreDocumento;

    @NotBlank
    @Size(max = 500)
    @Column(name = "url_descargada", nullable = false)
    private String urlDescargada;

    @CreatedDate
    @Column(name = "fecha_descarga", nullable = false, updatable = false)
    private LocalDateTime fechaDescarga;

    public enum TipoDocumento {
        ESTUDIO, NORMATIVA
    }
}

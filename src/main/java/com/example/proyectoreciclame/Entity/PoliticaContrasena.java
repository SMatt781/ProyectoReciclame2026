package com.example.proyectoreciclame.Entity;

import com.example.proyectoreciclame.Entity.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "politica_contrasena")
@EntityListeners(AuditingEntityListener.class)
public class PoliticaContrasena {

    @Id
    @Column(name = "id_politica")
    private Integer idPolitica;

    @NotNull
    @Column(name = "longitud_minima", nullable = false)
    private Integer longitudMinima = 8;

    @NotNull
    @Column(name = "requiere_mayuscula", nullable = false)
    private Boolean requiereMayuscula = true;

    @NotNull
    @Column(name = "requiere_numero", nullable = false)
    private Boolean requiereNumero = true;

    @NotNull
    @Column(name = "requiere_simbolo", nullable = false)
    private Boolean requiereSimbolo = true;

    @NotNull
    @Column(name = "mandato_mfa", nullable = false)
    private Boolean mandatoMfa = false;

    @Column(name = "expiracion_dias")
    private Integer expiracionDias;

    @ManyToOne
    @JoinColumn(name = "actualizado_por", nullable = false)
    private Usuario actualizadoPor;

    @LastModifiedDate
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}

package com.example.proyectoreciclame.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "indicador_valor")
@EntityListeners(AuditingEntityListener.class)
public class IndicadorValor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_valor")
    private Long idValor;

    @ManyToOne
    @JoinColumn(name = "id_indicador", nullable = false)
    private Indicador indicador;

    @NotNull
    @Column(name = "valor", nullable = false, precision = 15, scale = 4)
    private BigDecimal valor;

    @NotNull
    @Column(name = "anio", nullable = false)
    private Integer anio;

    @CreatedDate
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}

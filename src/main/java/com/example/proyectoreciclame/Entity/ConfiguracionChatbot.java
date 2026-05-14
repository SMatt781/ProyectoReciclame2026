package com.example.proyectoreciclame.Entity;

import com.example.proyectoreciclame.Entity.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "configuracion_chatbot")
@EntityListeners(AuditingEntityListener.class)
public class ConfiguracionChatbot {

    @Id
    @Column(name = "id_chatbot")
    private Integer idChatbot;

    @NotNull
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Size(max = 100)
    @Column(name = "proveedor")
    private String proveedor;

    @Column(name = "tasa_resolucion_pct", precision = 5, scale = 2)
    private BigDecimal tasaResolucionPct;

    @Column(name = "tiempo_respuesta_seg", precision = 5, scale = 2)
    private BigDecimal tiempoRespuestaSeg;

    @NotNull
    @Column(name = "consultas_hoy", nullable = false)
    private Integer consultasHoy = 0;

    @ManyToOne
    @JoinColumn(name = "actualizado_por", nullable = false)
    private Usuario actualizadoPor;

    @LastModifiedDate
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}

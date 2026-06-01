package com.example.proyectoreciclame.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "ai_usage_log")
@Getter @Setter @NoArgsConstructor
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long idLog;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "tipo_consulta", length = 30, nullable = false)
    private String tipoConsulta;   // "RESUMEN" | "CHATBOT"

    @Column(name = "fecha_consulta", nullable = false)
    private LocalDate fechaConsulta;

    @Column(name = "tokens_usados")
    private Integer tokensUsados;

    public AiUsageLog(Long idUsuario, String tipoConsulta, int tokensUsados) {
        this.idUsuario     = idUsuario;
        this.tipoConsulta  = tipoConsulta;
        this.fechaConsulta = LocalDate.now();
        this.tokensUsados  = tokensUsados;
    }
}

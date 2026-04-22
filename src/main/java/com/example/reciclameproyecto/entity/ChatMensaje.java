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
@Table(name = "chat_mensaje")
@EntityListeners(AuditingEntityListener.class)
public class ChatMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long idMensaje;

    @ManyToOne
    @JoinColumn(name = "id_chat", nullable = false)
    private ChatSesion chat;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "emisor", nullable = false)
    private EmisorMensaje emisor;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "mensaje", nullable = false)
    private String mensaje;

    @CreatedDate
    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    public enum EmisorMensaje {
        USUARIO, SISTEMA
    }
}

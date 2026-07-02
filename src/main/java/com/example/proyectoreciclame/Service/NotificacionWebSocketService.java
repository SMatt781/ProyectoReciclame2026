package com.example.proyectoreciclame.Service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificacionWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificacionWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void enviarTopico(String topico, Object payload) {
        try {
            messagingTemplate.convertAndSend(topico, payload);
        } catch (Exception e) {
            System.err.println("Error enviando WebSocket a topico " + topico + ": " + e.getMessage());
        }
    }

    public void enviarNotificacion(String correoUsuario, String titulo, String mensaje, String tipo) {
        try {
            Map<String, String> payload = Map.of(
                    "titulo", titulo != null ? titulo : "",
                    "mensaje", mensaje != null ? mensaje : "",
                    "tipo", tipo != null ? tipo : "INFO"
            );
            messagingTemplate.convertAndSendToUser(correoUsuario, "/queue/notificaciones", payload);
        } catch (Exception e) {
            System.err.println("Error enviando WebSocket a " + correoUsuario + ": " + e.getMessage());
        }
    }

    public void enviarSesionRevocada(String correoUsuario, String motivo) {
        try {
            Map<String, String> payload = Map.of(
                    "titulo", "Sesion cerrada",
                    "mensaje", motivo != null ? motivo : "Tu sesion fue cerrada por seguridad.",
                    "tipo", "SESSION_REVOKED",
                    "redirect", "/login?sessionRevoked=true"
            );
            messagingTemplate.convertAndSendToUser(correoUsuario, "/queue/notificaciones", payload);
            messagingTemplate.convertAndSendToUser(correoUsuario, "/queue/session-control", payload);
        } catch (Exception e) {
            System.err.println("Error enviando cierre de sesion a " + correoUsuario + ": " + e.getMessage());
        }
    }
}

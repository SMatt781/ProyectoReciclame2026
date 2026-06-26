package com.example.proyectoreciclame.Service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UsuariosActivosScheduler {

    private final SessionStore sessionStore;
    private final NotificacionWebSocketService webSocketService;

    public UsuariosActivosScheduler(SessionStore sessionStore,
                                    NotificacionWebSocketService webSocketService) {
        this.sessionStore = sessionStore;
        this.webSocketService = webSocketService;
    }

    @Scheduled(fixedDelay = 30000)
    public void broadcastUsuariosActivos() {
        int count = sessionStore.getCount();
        webSocketService.enviarTopico("/topic/admin/conectados", Map.of("count", count));
    }
}

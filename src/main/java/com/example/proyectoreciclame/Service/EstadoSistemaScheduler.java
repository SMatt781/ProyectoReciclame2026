package com.example.proyectoreciclame.Service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EstadoSistemaScheduler {

    private final EstadoSistemaService estadoSistemaService;
    private final NotificacionWebSocketService webSocketService;

    public EstadoSistemaScheduler(EstadoSistemaService estadoSistemaService,
                                  NotificacionWebSocketService webSocketService) {
        this.estadoSistemaService = estadoSistemaService;
        this.webSocketService = webSocketService;
    }

    @Scheduled(fixedDelay = 15000)
    public void broadcastEstadoSistema() {
        webSocketService.enviarTopico("/topic/superadmin/estado", estadoSistemaService.calcularEstadoSistema());
    }
}

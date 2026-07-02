package com.example.proyectoreciclame.Service;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStore implements HttpSessionListener {

    private final Set<HttpSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        sessions.add(se.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        sessions.remove(se.getSession());
    }

    public int getCount() {
        return sessions.size();
    }

    public int invalidarTodas(String excluirId) {
        int count = (int) sessions.stream().filter(s -> !s.getId().equals(excluirId)).count();
        sessions.removeIf(s -> {
            if (s.getId().equals(excluirId)) return false;
            try { s.invalidate(); } catch (Exception ignored) {}
            return true;
        });
        return count;
    }

    public int invalidarPorCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return 0;
        }

        String correoNormalizado = correo.trim().toLowerCase();
        int count = (int) sessions.stream()
                .filter(session -> correoNormalizado.equals(obtenerCorreoSesion(session)))
                .count();

        sessions.removeIf(session -> {
            if (!correoNormalizado.equals(obtenerCorreoSesion(session))) {
                return false;
            }
            try { session.invalidate(); } catch (Exception ignored) {}
            return true;
        });

        return count;
    }

    public void invalidarPorCorreoConRetraso(String correo, long retrasoMs) {
        if (correo == null || correo.isBlank()) {
            return;
        }

        Thread invalidacion = new Thread(() -> {
            try {
                Thread.sleep(Math.max(retrasoMs, 0));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            invalidarPorCorreo(correo);
        }, "session-invalidator");
        invalidacion.setDaemon(true);
        invalidacion.start();
    }

    private String obtenerCorreoSesion(HttpSession session) {
        try {
            Object context = session.getAttribute("SPRING_SECURITY_CONTEXT");
            if (context instanceof SecurityContext securityContext) {
                Authentication authentication = securityContext.getAuthentication();
                if (authentication != null && authentication.getName() != null) {
                    return authentication.getName().trim().toLowerCase();
                }
            }
        } catch (IllegalStateException ignored) {
        }
        return null;
    }
}

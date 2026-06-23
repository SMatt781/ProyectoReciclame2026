package com.example.proyectoreciclame.Service;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
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

    public int invalidarTodas(String excluirId) {
        int count = (int) sessions.stream().filter(s -> !s.getId().equals(excluirId)).count();
        sessions.removeIf(s -> {
            if (s.getId().equals(excluirId)) return false;
            try { s.invalidate(); } catch (Exception ignored) {}
            return true;
        });
        return count;
    }
}

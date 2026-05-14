package com.example.proyectoreciclame.Security;

import com.example.proyectoreciclame.Service.LoginAuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final LoginAuditoriaService loginAuditoriaService;

    public CustomAuthenticationFailureHandler(LoginAuditoriaService loginAuditoriaService) {
        this.loginAuditoriaService = loginAuditoriaService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String correo = request.getParameter("correo");

        if (correo != null && !correo.isBlank()) {
            correo = correo.trim().toLowerCase();

            // Registra el intento fallido y devuelve cuántos intentos fallidos
            // se hicieron en los últimos 15 minutos para ese correo
            long fallidosRecientes = loginAuditoriaService.registrarIntentoFallido(correo, request);

            // Si ya fue bloqueado (>= 5 intentos) → redirige con parámetro bloqueado
            if (fallidosRecientes >= 5) {
                response.sendRedirect("/login?error=bloqueado&correo="
                        + URLEncoder.encode(correo, StandardCharsets.UTF_8));
                return;
            }

            // Si está entre 3 y 4 intentos → avisa cuántos quedan
            if (fallidosRecientes >= 3) {
                long intentosRestantes = 5 - fallidosRecientes;
                response.sendRedirect("/login?error=intentos&restantes=" + intentosRestantes
                        + "&correo=" + URLEncoder.encode(correo, StandardCharsets.UTF_8));
                return;
            }
        }

        // Menos de 3 intentos → error genérico
        response.sendRedirect("/login?error=true");
    }
}

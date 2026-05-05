package com.example.proyectoreciclame.Security;

import com.example.proyectoreciclame.Service.LoginAuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
            loginAuditoriaService.registrarIntentoFallido(correo.trim().toLowerCase(), request);
        }

        response.sendRedirect("/login?error=true");
    }
}
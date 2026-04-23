package com.example.proyectoreciclame.Security;

import com.example.proyectoreciclame.Service.LoginAuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginAuditoriaService loginAuditoriaService;

    public CustomAuthenticationSuccessHandler(LoginAuditoriaService loginAuditoriaService) {
        this.loginAuditoriaService = loginAuditoriaService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String correo = authentication.getName();
        loginAuditoriaService.registrarIntentoExitoso(correo, request);

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN"));
        boolean esSocio = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SOCIO"));

        if (esAdmin) {
            response.sendRedirect("/admin/dashboard");
        } else if (esSocio) {
            response.sendRedirect("/socio");
        } else {
            response.sendRedirect("/estudios");
        }
    }
}
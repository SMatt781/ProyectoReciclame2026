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

        boolean esSuperadmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean esSocio = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SOCIO"));

        boolean esVisualizador = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VISUALIZADOR"));

        if (esSuperadmin) {
            response.sendRedirect("/superadmin/dashboard");
        } else if (esAdmin) {
            response.sendRedirect("/admin/dashboard");
        } else if (esSocio) {
            response.sendRedirect("/socio");
        } else if (esVisualizador) {
            response.sendRedirect("/visualizador");
        } else {
            response.sendRedirect("/login");
        }
    }
}
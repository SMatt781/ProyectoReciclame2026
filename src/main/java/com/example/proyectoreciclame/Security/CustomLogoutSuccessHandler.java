package com.example.proyectoreciclame.Security;

import com.example.proyectoreciclame.Service.LoginAuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final LoginAuditoriaService loginAuditoriaService;

    public CustomLogoutSuccessHandler(LoginAuditoriaService loginAuditoriaService) {
        this.loginAuditoriaService = loginAuditoriaService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException {

        String correo = authentication != null ? authentication.getName() : null;
        loginAuditoriaService.cerrarSesion(request, correo);
        response.sendRedirect("/login?logout=true");
    }
}
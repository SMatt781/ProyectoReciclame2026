package com.example.proyectoreciclame.Security;

import com.example.proyectoreciclame.Service.RecaptchaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.io.IOException;

public class RecaptchaLoginFilter extends OncePerRequestFilter {

    private final RecaptchaService recaptchaService;

    public RecaptchaLoginFilter(RecaptchaService recaptchaService) {
        this.recaptchaService = recaptchaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        boolean esLoginPost = "/login".equals(request.getServletPath())
                && "POST".equalsIgnoreCase(request.getMethod());

        if (esLoginPost) {
            String tokenCaptcha = request.getParameter("g-recaptcha-response");
            String ipCliente = obtenerIpCliente(request);

            boolean captchaValido = recaptchaService.validarCaptcha(tokenCaptcha, ipCliente);

            if (!captchaValido) {
                String correo = request.getParameter("correo");

                String redirectUrl = request.getContextPath() + "/login?captchaError=true";

                if (correo != null && !correo.isBlank()) {
                    redirectUrl += "&correo=" + URLEncoder.encode(
                            correo.trim(),
                            StandardCharsets.UTF_8
                    );
                }

                response.sendRedirect(redirectUrl);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
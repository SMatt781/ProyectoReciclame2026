package com.example.proyectoreciclame.Security;

import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AccountStatusFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;

    public AccountStatusFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!estaAutenticado(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        Usuario usuario = usuarioRepository.findByCorreoWithRol(authentication.getName()).orElse(null);
        if (usuario == null || cuentaNoHabilitada(usuario) || rolCambio(authentication, usuario)) {
            cerrarSesion(request);
            borrarCookie(response, "JSESSIONID");
            borrarCookie(response, "remember-me");
            if (esAjax(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"authenticated\":false,\"redirect\":\"/login?sessionRevoked=true\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/login?sessionRevoked=true");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean estaAutenticado(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName());
    }

    private boolean cuentaNoHabilitada(Usuario usuario) {
        return usuario.getEliminadoEn() != null
                || !"APROBADO".equalsIgnoreCase(usuario.getEstadoAprobacion())
                || usuario.getEstadoCuenta() != Usuario.EstadoCuenta.ACTIVO;
    }

    private boolean rolCambio(Authentication authentication, Usuario usuario) {
        String rolActual = usuario.getRol() != null && usuario.getRol().getNombre() != null
                ? "ROLE_" + usuario.getRol().getNombre().toUpperCase()
                : "";

        return authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals(rolActual));
    }

    private void cerrarSesion(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            try { session.invalidate(); } catch (IllegalStateException ignored) {}
        }
    }

    private boolean esAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));
    }

    private void borrarCookie(HttpServletResponse response, String nombre) {
        Cookie cookie = new Cookie(nombre, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}

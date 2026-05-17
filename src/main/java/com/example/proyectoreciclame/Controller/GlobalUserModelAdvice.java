package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.SessionUserDto;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.NotificacionRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.AuthenticatedUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalUserModelAdvice {

    private final AuthenticatedUserService authenticatedUserService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;

    public GlobalUserModelAdvice(AuthenticatedUserService authenticatedUserService,
                                 UsuarioRepository usuarioRepository,
                                 NotificacionRepository notificacionRepository) {
        this.authenticatedUserService = authenticatedUserService;
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @ModelAttribute("sessionUser")
    public SessionUserDto sessionUser() {
        return authenticatedUserService.obtenerUsuarioSesion();
    }

    @ModelAttribute("notificacionesNoLeidas")
    public Long notificacionesNoLeidas() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return 0L;
        }

        Usuario usuario = usuarioRepository.findByCorreoWithRol(auth.getName()).orElse(null);
        if (usuario == null) {
            return 0L;
        }

        return notificacionRepository.countByUsuarioAndLeidoFalse(usuario);
    }

    @ModelAttribute("usuarioRol")
    public String usuarioRol() {
        SessionUserDto sessionUser = authenticatedUserService.obtenerUsuarioSesion();
        return sessionUser != null ? sessionUser.getRol() : null;
    }
}
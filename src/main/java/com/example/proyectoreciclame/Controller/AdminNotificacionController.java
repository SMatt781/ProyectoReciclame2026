package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.SessionUserDto;
import com.example.proyectoreciclame.Entity.Notificacion;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.NotificacionRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.AuthenticatedUserService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class AdminNotificacionController {

    private final AuthenticatedUserService authenticatedUserService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;

    public AdminNotificacionController(AuthenticatedUserService authenticatedUserService,
                                       UsuarioRepository usuarioRepository,
                                       NotificacionRepository notificacionRepository) {
        this.authenticatedUserService = authenticatedUserService;
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @GetMapping({"/admin/notificaciones", "/socio/notificaciones", "/visualizador/notificaciones"})
    public String verNotificaciones(Model model) {
        SessionUserDto sessionUser = authenticatedUserService.obtenerUsuarioSesion();
        if (sessionUser == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findByCorreoWithRol(sessionUser.getCorreo()).orElse(null);
        if (usuario == null) {
            return "redirect:/login";
        }

        // Recuperar notificaciones de BD
        List<Notificacion> notificaciones = notificacionRepository
                .findByUsuarioIdOrderByFechaDesc(usuario.getIdUsuario());

        long totalNoLeidas = notificacionRepository
                .countNoLeidasByUsuario(usuario.getIdUsuario());

        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("totalNoLeidas", totalNoLeidas);
        model.addAttribute("currentPage", "notificaciones");

        return "admin/notificaciones";
    }

    @PostMapping({"/admin/notificaciones/marcar-leidas", "/socio/notificaciones/marcar-leidas", "/visualizador/notificaciones/marcar-leidas"})
    @Transactional
    public String marcarTodasLeidas(RedirectAttributes redirectAttributes) {
        SessionUserDto sessionUser = authenticatedUserService.obtenerUsuarioSesion();
        if (sessionUser == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findByCorreoWithRol(sessionUser.getCorreo()).orElse(null);
        if (usuario != null) {
            notificacionRepository.findByUsuarioIdOrderByFechaDesc(usuario.getIdUsuario())
                    .forEach(n -> {
                        n.setLeido(true);
                        notificacionRepository.save(n);
                    });
        }

        redirectAttributes.addFlashAttribute("success", "Todas las notificaciones marcadas como leídas.");
        return "redirect:/" + sessionUser.getRol().toLowerCase() + "/notificaciones";
    }

    @PostMapping({"/admin/notificaciones/marcar-leida/{id}", "/socio/notificaciones/marcar-leida/{id}", "/visualizador/notificaciones/marcar-leida/{id}"})
    public String marcarLeida(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        SessionUserDto sessionUser = authenticatedUserService.obtenerUsuarioSesion();
        if (sessionUser == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findByCorreoWithRol(sessionUser.getCorreo()).orElse(null);
        if (usuario == null) {
            return "redirect:/login";
        }

        notificacionRepository.findById(id).ifPresent(n -> {
            if (n.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
                n.setLeido(true);
                notificacionRepository.save(n);
            }
        });

        redirectAttributes.addFlashAttribute("success", "Notificación marcada como leída.");
        return "redirect:/" + sessionUser.getRol().toLowerCase() + "/notificaciones";
    }

    // ──────────────────────────────────────────────────────────────────
    // Métodos auxiliares para crear notificaciones por rol
    // ──────────────────────────────────────────────────────────────────

    public void crearNotificacionAdmin(String titulo, String mensaje, String tipo, String enlace) {
        crearNotificacionPorRol(List.of(2), titulo, mensaje, tipo, enlace);
    }

    public void crearNotificacionSocio(String titulo, String mensaje, String tipo, String enlace) {
        crearNotificacionPorRol(List.of(3), titulo, mensaje, tipo, enlace);
    }

    public void crearNotificacionVisualizador(String titulo, String mensaje, String tipo, String enlace) {
        crearNotificacionPorRol(List.of(4), titulo, mensaje, tipo, enlace);
    }

    private void crearNotificacionPorRol(List<Integer> rolIds, String titulo, String mensaje, String tipo, String enlace) {
        try {
            usuarioRepository.findByRolIdInAndEliminadoEnIsNull(rolIds)
                    .forEach(usuario -> {
                        Notificacion n = new Notificacion();
                        n.setUsuario(usuario);
                        n.setTitulo(titulo);
                        n.setMensaje(mensaje);
                        n.setTipo(tipo);
                        n.setEnlaceReferencia(enlace);
                        n.setLeido(false);
                        n.setFecha(LocalDateTime.now());
                        notificacionRepository.save(n);
                    });
        } catch (Exception e) {
            System.out.println("ERROR notificación: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
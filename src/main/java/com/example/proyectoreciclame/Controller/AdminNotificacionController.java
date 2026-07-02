package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.SessionUserDto;
import com.example.proyectoreciclame.Entity.Notificacion;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.NotificacionRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.AuthenticatedUserService;
import com.example.proyectoreciclame.Service.NotificacionWebSocketService;
import com.example.proyectoreciclame.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AdminNotificacionController {

    private final AuthenticatedUserService authenticatedUserService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionWebSocketService webSocketService;

    public AdminNotificacionController(AuthenticatedUserService authenticatedUserService,
                                       UsuarioRepository usuarioRepository,
                                       NotificacionRepository notificacionRepository,
                                       NotificacionWebSocketService webSocketService) {
        this.authenticatedUserService = authenticatedUserService;
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
        this.webSocketService = webSocketService;
    }

    @GetMapping("/api/notificaciones/no-leidas")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> conteoNoLeidas() {
        SessionUserDto sessionUser = authenticatedUserService.obtenerUsuarioSesion();
        if (sessionUser == null) return ResponseEntity.ok(java.util.Map.of("count", 0));
        Usuario usuario = usuarioRepository.findByCorreoWithRol(sessionUser.getCorreo()).orElse(null);
        if (usuario == null) return ResponseEntity.ok(java.util.Map.of("count", 0));
        long count = notificacionRepository.countByUsuarioAndLeidoFalse(usuario);
        return ResponseEntity.ok(java.util.Map.of("count", count));
    }

    @GetMapping({"/admin/notificaciones", "/socio/notificaciones", "/visualizador/notificaciones"})
    public String verNotificaciones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "ALL") String tipo,
            Model model) {
        SessionUserDto sessionUser = authenticatedUserService.obtenerUsuarioSesion();
        if (sessionUser == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findByCorreoWithRol(sessionUser.getCorreo()).orElse(null);
        if (usuario == null) {
            return "redirect:/login";
        }

        // Paginación: 5 notificaciones por página
        Pageable pageable = PageRequest.of(page, 5);
        Page<Notificacion> paginaNotificaciones;

        // Filtrar por tipo si no es "ALL"
        if ("ALL".equals(tipo)) {
            paginaNotificaciones = notificacionRepository
                    .findByUsuarioIdPaginado(usuario.getIdUsuario(), pageable);
        } else {
            paginaNotificaciones = notificacionRepository
                    .findByUsuarioIdAndTipoPaginado(usuario.getIdUsuario(), tipo, pageable);
        }

        long totalNoLeidas = notificacionRepository
                .countNoLeidasByUsuario(usuario.getIdUsuario());

        model.addAttribute("notificaciones", paginaNotificaciones.getContent());
        model.addAttribute("totalNoLeidas", totalNoLeidas);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paginaNotificaciones.getTotalPages());
        model.addAttribute("hasPrevious", paginaNotificaciones.hasPrevious());
        model.addAttribute("hasNext", paginaNotificaciones.hasNext());
        model.addAttribute("pageNumbers", PaginationUtils.buildPageNumbers(page, paginaNotificaciones.getTotalPages()));
        model.addAttribute("currentPageName", "notificaciones");
        model.addAttribute("tipoFiltro", tipo);

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
    // Notificación masiva (solo ADMIN)
    // ──────────────────────────────────────────────────────────────────

    @GetMapping("/admin/notificaciones/masiva")
    public String mostrarFormularioMasivo(Model model) {
        model.addAttribute("currentSection", "admin-notificacion-masiva");
        model.addAttribute("currentPageName", "notificacion-masiva");
        return "admin/notificaciones-masivas";
    }

    @PostMapping("/admin/notificaciones/masiva")
    public String enviarNotificacionMasiva(
            @RequestParam("titulo") String titulo,
            @RequestParam("mensaje") String mensaje,
            @RequestParam(value = "tipo", defaultValue = "INFO") String tipo,
            @RequestParam(value = "enlace", required = false) String enlace,
            @RequestParam(value = "roles", required = false) List<Integer> roles,
            RedirectAttributes redirectAttributes) {

        if (roles == null || roles.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Debes seleccionar al menos un rol destinatario.");
            return "redirect:/admin/notificaciones/masiva";
        }

        // Solo permitir roles ADMIN(2), SOCIO(3), VISUALIZADOR(4) — excluir SUPERADMIN(1)
        List<Integer> rolesPermitidos = roles.stream()
                .filter(r -> r == 2 || r == 3 || r == 4)
                .collect(Collectors.toList());

        if (rolesPermitidos.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Rol no permitido seleccionado.");
            return "redirect:/admin/notificaciones/masiva";
        }

        crearNotificacionPorRol(rolesPermitidos,
                titulo != null ? titulo.trim() : "",
                mensaje != null ? mensaje.trim() : "",
                tipo,
                enlace != null && !enlace.isBlank() ? enlace.trim() : null);

        redirectAttributes.addFlashAttribute("exito", "Notificación enviada correctamente a los roles seleccionados.");
        return "redirect:/admin/notificaciones/masiva";
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

    public void crearNotificacionPorUsuario(Long idUsuario, String titulo, String mensaje, String tipo, String enlace) {
        try {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            if (usuario != null) {
                Notificacion n = new Notificacion();
                n.setUsuario(usuario);
                n.setTitulo(titulo);
                n.setMensaje(mensaje);
                n.setTipo(tipo);
                n.setEnlaceReferencia(enlace);
                n.setLeido(false);
                n.setFecha(LocalDateTime.now());
                notificacionRepository.save(n);
                webSocketService.enviarNotificacion(usuario.getCorreo(), titulo, mensaje, tipo);
            }
        } catch (Exception e) {
            System.out.println("ERROR notificación por usuario: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void crearNotificacionPorRol(List<Integer> rolIds, String titulo, String mensaje, String tipo, String enlace) {
        try {
            var usuarios = usuarioRepository.findByRolIdInAndEliminadoEnIsNull(rolIds);
            System.out.println("[NOTIF] Enviando a " + usuarios.size() + " usuarios con rolIds=" + rolIds);
            usuarios.forEach(usuario -> {
                Notificacion n = new Notificacion();
                n.setUsuario(usuario);
                n.setTitulo(titulo);
                n.setMensaje(mensaje);
                n.setTipo(tipo);
                n.setEnlaceReferencia(enlace);
                n.setLeido(false);
                n.setFecha(LocalDateTime.now());
                notificacionRepository.save(n);
                System.out.println("[NOTIF] WS → " + usuario.getCorreo() + " | " + titulo);
                webSocketService.enviarNotificacion(usuario.getCorreo(), titulo, mensaje, tipo);
            });
        } catch (Exception e) {
            System.out.println("ERROR notificación: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
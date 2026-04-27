package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.SolicitudRegistroDto;
import com.example.proyectoreciclame.Entity.SolicitudRegistro;
import com.example.proyectoreciclame.Repository.SolicitudRegistroRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.UsuarioRepository;

import java.util.Optional;

@Controller
@RequestMapping("/admin/usuarios/solicitudes")
public class AdminSolicitudController {

    private final SolicitudRegistroRepository solicitudRegistroRepository;
    private final UsuarioRepository usuarioRepository;

    public AdminSolicitudController(SolicitudRegistroRepository solicitudRegistroRepository,
                                    UsuarioRepository usuarioRepository) {
        this.solicitudRegistroRepository = solicitudRegistroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listarSolicitudes(@RequestParam(defaultValue = "0") int page, Model model) {

        Pageable pageable = PageRequest.of(page, 5);
        Page<Usuario> pagina = usuarioRepository
                .findByEstadoAprobacionAndEliminadoEnIsNullOrderByFechaRegistroDesc("PENDIENTE", pageable);

        List<SolicitudRegistroDto> solicitudes = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        for (Usuario u : pagina.getContent()) {

            Optional<SolicitudRegistro> solicitudOpt = buscarSolicitudRelacionada(u);

            String rolSolicitado = (u.getRol() != null && u.getRol().getNombre() != null)
                    ? u.getRol().getNombre()
                    : solicitudOpt.map(SolicitudRegistro::getRolSolicitado).orElse("Sin rol");

            String fecha = solicitudOpt
                    .map(SolicitudRegistro::getFechaSolicitud)
                    .map(f -> f.format(formatter))
                    .orElse(u.getFechaRegistro() != null ? u.getFechaRegistro().format(formatter) : "-");

            String nombreCompleto = construirNombreCompleto(
                    u.getNombres(),
                    u.getApellidoPaterno(),
                    u.getApellidoMaterno()
            );

            solicitudes.add(new SolicitudRegistroDto(
                    u.getIdUsuario(),
                    nombreCompleto,
                    u.getCorreo(),
                    u.getDni(),
                    rolSolicitado,
                    u.getEstadoAprobacion(),
                    fecha,
                    obtenerIniciales(u.getNombres(), u.getApellidoPaterno())
            ));
        }

        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("currentSection", "admin-usuarios");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pagina.getTotalPages());
        model.addAttribute("hasPrevious", pagina.hasPrevious());
        model.addAttribute("hasNext", pagina.hasNext());
        model.addAttribute("totalPendientes", pagina.getTotalElements());

        return "admin/solicitudes-registro";
    }

    @GetMapping("/{idUsuario}")
    public String detalleSolicitud(@PathVariable Long idUsuario, Model model) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);

        if (usuario == null) {
            return "redirect:/admin/usuarios/solicitudes";
        }

        Optional<SolicitudRegistro> solicitudOpt = buscarSolicitudRelacionada(usuario);

        model.addAttribute("usuario", usuario);
        model.addAttribute("solicitudExtra", solicitudOpt.orElse(null));
        model.addAttribute("currentSection", "admin-usuarios");

        return "admin/detalle-solicitud";
    }

    @PostMapping("/{idUsuario}/aceptar")
    public String aceptarSolicitud(@PathVariable Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);

        if (usuario != null) {
            usuario.setEstadoAprobacion("APROBADO");
            usuario.setEstadoCuenta(Usuario.EstadoCuenta.ACTIVO);
            usuario.setActualizadoEn(LocalDateTime.now());
            usuarioRepository.save(usuario);

            Optional<SolicitudRegistro> solicitudOpt = buscarSolicitudRelacionada(usuario);
            if (solicitudOpt.isPresent()) {
                SolicitudRegistro solicitud = solicitudOpt.get();
                solicitud.setEstado("APROBADO");
                solicitud.setFechaResolucion(LocalDateTime.now());
                solicitudRegistroRepository.save(solicitud);
            }
        }

        return "redirect:/admin/usuarios/solicitudes";
    }

    @PostMapping("/{idUsuario}/denegar")
    public String denegarSolicitud(@PathVariable Long idUsuario,
                                   @RequestParam(required = false) String motivo) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);

        if (usuario != null) {
            usuario.setEstadoAprobacion("RECHAZADO");
            usuario.setEstadoCuenta(null);
            usuario.setActualizadoEn(LocalDateTime.now());
            usuarioRepository.save(usuario);

            Optional<SolicitudRegistro> solicitudOpt = buscarSolicitudRelacionada(usuario);
            if (solicitudOpt.isPresent()) {
                SolicitudRegistro solicitud = solicitudOpt.get();
                solicitud.setEstado("RECHAZADO");
                solicitud.setMotivoRechazo(motivo);
                solicitud.setFechaResolucion(LocalDateTime.now());
                solicitudRegistroRepository.save(solicitud);
            }
        }

        return "redirect:/admin/usuarios/solicitudes";
    }

    private Optional<SolicitudRegistro> buscarSolicitudRelacionada(Usuario usuario) {
        Optional<SolicitudRegistro> porDni = solicitudRegistroRepository
                .findTopByDniOrderByFechaSolicitudDesc(usuario.getDni());

        if (porDni.isPresent()) {
            return porDni;
        }

        return solicitudRegistroRepository.findTopByCorreoOrderByFechaSolicitudDesc(usuario.getCorreo());
    }

    private String construirNombreCompleto(String nombres, String apellidoPaterno, String apellidoMaterno) {
        StringBuilder sb = new StringBuilder();
        if (nombres != null) sb.append(nombres);
        if (apellidoPaterno != null) sb.append(" ").append(apellidoPaterno);
        if (apellidoMaterno != null && !apellidoMaterno.isBlank()) sb.append(" ").append(apellidoMaterno);
        return sb.toString().trim();
    }

    private String obtenerIniciales(String nombres, String apellidoPaterno) {
        String n = (nombres != null && !nombres.isBlank()) ? nombres.substring(0, 1).toUpperCase() : "";
        String a = (apellidoPaterno != null && !apellidoPaterno.isBlank()) ? apellidoPaterno.substring(0, 1).toUpperCase() : "";
        return n + a;
    }
}
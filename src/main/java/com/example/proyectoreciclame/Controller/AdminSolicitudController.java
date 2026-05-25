package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.SolicitudRegistroDto;
import com.example.proyectoreciclame.Entity.HistorialRoles;
import com.example.proyectoreciclame.Entity.SolicitudRegistro;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.HistorialRolesRepository;
import com.example.proyectoreciclame.Repository.SolicitudRegistroRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.CorreoService;
import com.example.proyectoreciclame.util.PaginationUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/usuarios/solicitudes")
public class AdminSolicitudController {

    private final SolicitudRegistroRepository solicitudRegistroRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorreoService correoService;
    private final HistorialRolesRepository historialRolesRepository;
    private final AdminNotificacionController adminNotificacionController;

    public AdminSolicitudController(SolicitudRegistroRepository solicitudRegistroRepository,
                                    UsuarioRepository usuarioRepository,
                                    CorreoService correoService,
                                    HistorialRolesRepository historialRolesRepository,
                                    AdminNotificacionController adminNotificacionController) {
        this.solicitudRegistroRepository = solicitudRegistroRepository;
        this.usuarioRepository = usuarioRepository;
        this.correoService = correoService;
        this.historialRolesRepository = historialRolesRepository;
        this.adminNotificacionController = adminNotificacionController;
    }

    @GetMapping
    public String listarSolicitudes(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) String rol,
                                    @RequestParam(required = false) String dateStart,
                                    @RequestParam(required = false) String dateEnd,
                                    Model model) {

        Pageable pageable = PageRequest.of(page, 5);

        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        String rolParam = (rol != null && !rol.isBlank()) ? rol.trim() : null;

        LocalDateTime fechaInicio = (dateStart != null && !dateStart.isBlank())
                ? LocalDate.parse(dateStart).atStartOfDay() : null;

        LocalDateTime fechaFin = (dateEnd != null && !dateEnd.isBlank())
                ? LocalDate.parse(dateEnd).atTime(23, 59, 59) : null;

        Page<Usuario> pagina = usuarioRepository.filtrarSolicitudesPendientes(
                searchParam, rolParam, fechaInicio, fechaFin, pageable);

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

            String nombreCompleto = construirNombreCompleto(u.getNombres(), u.getApellidoPaterno(), u.getApellidoMaterno());

            String tipoIdentificacion = null;
            String numeroIdentificacion = null;
            if (u.getIdentificacion() != null) {
                tipoIdentificacion = u.getIdentificacion().getTipo().name();
                numeroIdentificacion = u.getIdentificacion().getNumero();
            }

            solicitudes.add(new SolicitudRegistroDto(
                    u.getIdUsuario(), nombreCompleto, u.getCorreo(),
                    tipoIdentificacion, numeroIdentificacion,
                    rolSolicitado, u.getEstadoAprobacion(), fecha,
                    obtenerIniciales(u.getNombres(), u.getApellidoPaterno())
            ));
        }

        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = hoy.atTime(23, 59, 59);

        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("currentSection", "admin-usuarios");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pagina.getTotalPages());
        model.addAttribute("hasPrevious", pagina.hasPrevious());
        model.addAttribute("hasNext", pagina.hasNext());
        model.addAttribute("pageNumbers", PaginationUtils.buildPageNumbers(page, pagina.getTotalPages()));
        model.addAttribute("totalPendientes", usuarioRepository.countByEstadoAprobacionAndEliminadoEnIsNull("PENDIENTE"));
        model.addAttribute("totalUsuariosActivos", usuarioRepository.countUsuariosActivosAprobados());
        model.addAttribute("empresasRegistradas", usuarioRepository.countEmpresasRegistradas());
        model.addAttribute("solicitudesHoy", solicitudRegistroRepository.countByEstadoAndFechaSolicitudBetween("PENDIENTE", inicioHoy, finHoy));
        model.addAttribute("search", search);
        model.addAttribute("rolSeleccionado", rol);
        model.addAttribute("dateStart", dateStart);
        model.addAttribute("dateEnd", dateEnd);

        return "admin/solicitudes-registro";
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarSolicitudes(@RequestParam(required = false) String search,
                                                      @RequestParam(required = false) String rol,
                                                      @RequestParam(required = false) String dateStart,
                                                      @RequestParam(required = false) String dateEnd) throws IOException {

        Pageable pageable = PageRequest.of(0, 10000);

        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        String rolParam = (rol != null && !rol.isBlank()) ? rol.trim() : null;

        LocalDateTime fechaInicio = (dateStart != null && !dateStart.isBlank())
                ? LocalDate.parse(dateStart).atStartOfDay() : null;

        LocalDateTime fechaFin = (dateEnd != null && !dateEnd.isBlank())
                ? LocalDate.parse(dateEnd).atTime(23, 59, 59) : null;

        Page<Usuario> pagina = usuarioRepository.filtrarSolicitudesPendientes(
                searchParam, rolParam, fechaInicio, fechaFin, pageable);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Solicitudes");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Nombre");
        header.createCell(1).setCellValue("DNI");
        header.createCell(2).setCellValue("Correo");
        header.createCell(3).setCellValue("Empresa");
        header.createCell(4).setCellValue("Rol solicitado");
        header.createCell(5).setCellValue("Estado");
        header.createCell(6).setCellValue("Fecha solicitud");

        int rowNum = 1;

        for (Usuario u : pagina.getContent()) {
            Optional<SolicitudRegistro> solicitudOpt = buscarSolicitudRelacionada(u);

            String nombreCompleto = construirNombreCompleto(u.getNombres(), u.getApellidoPaterno(), u.getApellidoMaterno());
            String empresa = u.getUsuarioEmpresa() != null ? u.getUsuarioEmpresa().getRazonSocial() : "--";
            String rolSolicitado = (u.getRol() != null && u.getRol().getNombre() != null)
                    ? u.getRol().getNombre()
                    : solicitudOpt.map(SolicitudRegistro::getRolSolicitado).orElse("Sin rol");
            String fecha = solicitudOpt.map(SolicitudRegistro::getFechaSolicitud)
                    .map(LocalDateTime::toString)
                    .orElse(u.getFechaRegistro() != null ? u.getFechaRegistro().toString() : "-");

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(nombreCompleto);
            row.createCell(1).setCellValue(u.getIdentificacion() != null ? u.getIdentificacion().getNumero() : "-");
            row.createCell(2).setCellValue(u.getCorreo() != null ? u.getCorreo() : "-");
            row.createCell(3).setCellValue(empresa);
            row.createCell(4).setCellValue(rolSolicitado);
            row.createCell(5).setCellValue(u.getEstadoAprobacion() != null ? u.getEstadoAprobacion() : "-");
            row.createCell(6).setCellValue(fecha);
        }

        for (int i = 0; i <= 6; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=solicitudes_registro.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(out.toByteArray());
    }

    @GetMapping("/{idUsuario}")
    public String detalleSolicitud(@PathVariable Long idUsuario, Model model) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) return "redirect:/admin/usuarios/solicitudes";

        Optional<SolicitudRegistro> solicitudOpt = buscarSolicitudRelacionada(usuario);
        model.addAttribute("usuario", usuario);
        model.addAttribute("solicitudExtra", solicitudOpt.orElse(null));
        model.addAttribute("currentSection", "admin-usuarios");
        return "admin/detalle-solicitud";
    }

    @PostMapping("/{idUsuario}/aceptar")
    public String aceptarSolicitud(@PathVariable Long idUsuario,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {

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

            // ── NUEVO: registrar en historial ─────────────────────────────────
            Usuario adminQueAprueba = usuarioRepository
                    .findByCorreoWithRol(authentication.getName()).orElse(null);

            HistorialRoles historial = new HistorialRoles();
            historial.setUsuarioAfectado(usuario);
            historial.setRolAnterior(null);
            historial.setRolNuevo(usuario.getRol());
            historial.setEstadoAnterior(null);
            historial.setEstadoNuevo(Usuario.EstadoCuenta.ACTIVO);
            historial.setAutorizadoPor(adminQueAprueba);
            historial.setMotivo("Solicitud de registro aprobada");
            historialRolesRepository.save(historial);
            // ─────────────────────────────────────────────────────────────────

            correoService.enviarRegistroAprobado(usuario.getCorreo(), usuario.getNombres());

            // ── Crear notificación para el usuario ─────────────────────────────
            String rolSolicitado = usuario.getRol() != null ? usuario.getRol().getNombre() : "USUARIO";
            adminNotificacionController.crearNotificacionPorUsuario(
                    usuario.getIdUsuario(),
                    "¡Tu solicitud ha sido aprobada!",
                    "Tu cuenta como " + rolSolicitado + " ha sido aprobada. Ahora puedes iniciar sesión.",
                    "REGISTRO_APROBADO",
                    "/login"
            );
            // ──────────────────────────────────────────────────────────────────

            // ── Flash message ─────────────────────────────────────────────────
            redirectAttributes.addFlashAttribute("success", "Solicitud aceptada correctamente. Usuario creado y activado.");
            // ──────────────────────────────────────────────────────────────────
        }

        return "redirect:/admin/usuarios/solicitudes";
    }

    @PostMapping("/{idUsuario}/denegar")
    public String denegarSolicitud(@PathVariable Long idUsuario,
                                   @RequestParam(required = false) String motivo,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {

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

            // ── NUEVO: registrar en historial ─────────────────────────────────
            Usuario adminQueActua = usuarioRepository
                    .findByCorreoWithRol(authentication.getName()).orElse(null);

            HistorialRoles historial = new HistorialRoles();
            historial.setUsuarioAfectado(usuario);
            historial.setRolAnterior(null);
            historial.setRolNuevo(null);
            historial.setEstadoAnterior(null);
            historial.setEstadoNuevo(null);
            historial.setAutorizadoPor(adminQueActua);
            historial.setMotivo(motivo != null && !motivo.isBlank() ? motivo : "Solicitud de registro denegada");
            historialRolesRepository.save(historial);
            // ─────────────────────────────────────────────────────────────────

            correoService.enviarRegistroDenegado(usuario.getCorreo(), usuario.getNombres(), motivo);

            // ── Crear notificación para el usuario ─────────────────────────────
            String mensajeRechazo = motivo != null && !motivo.isBlank()
                    ? "Lamentablemente, tu solicitud de registro ha sido rechazada. Motivo: " + motivo
                    : "Lamentablemente, tu solicitud de registro ha sido rechazada.";

            adminNotificacionController.crearNotificacionPorUsuario(
                    usuario.getIdUsuario(),
                    "Tu solicitud de registro ha sido rechazada",
                    mensajeRechazo,
                    "REGISTRO_RECHAZADO",
                    null
            );
            // ──────────────────────────────────────────────────────────────────

            // ── Flash message ─────────────────────────────────────────────────
            redirectAttributes.addFlashAttribute("success", "Solicitud denegada correctamente.");
            // ──────────────────────────────────────────────────────────────────
        }

        return "redirect:/admin/usuarios/solicitudes";
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private Optional<SolicitudRegistro> buscarSolicitudRelacionada(Usuario usuario) {
        String numeroId = usuario.getIdentificacion() != null ? usuario.getIdentificacion().getNumero() : null;
        if (numeroId != null && !numeroId.isBlank()) {
            Optional<SolicitudRegistro> porNumero = solicitudRegistroRepository
                    .findTopByNumeroIdentificacionOrderByFechaSolicitudDesc(numeroId);
            if (porNumero.isPresent()) return porNumero;
        }
        return solicitudRegistroRepository.findTopByCorreoOrderByFechaSolicitudDesc(usuario.getCorreo());
    }

    private String construirNombreCompleto(String nombres, String apellidoPaterno, String apellidoMaterno) {
        StringBuilder sb = new StringBuilder();
        if (nombres != null && !nombres.isBlank()) sb.append(nombres);
        if (apellidoPaterno != null && !apellidoPaterno.isBlank()) { if (!sb.isEmpty()) sb.append(" "); sb.append(apellidoPaterno); }
        if (apellidoMaterno != null && !apellidoMaterno.isBlank()) { if (!sb.isEmpty()) sb.append(" "); sb.append(apellidoMaterno); }
        return sb.toString().trim();
    }

    private String obtenerIniciales(String nombres, String apellidoPaterno) {
        String n = (nombres != null && !nombres.isBlank()) ? nombres.substring(0, 1).toUpperCase() : "";
        String a = (apellidoPaterno != null && !apellidoPaterno.isBlank()) ? apellidoPaterno.substring(0, 1).toUpperCase() : "";
        return n + a;
    }
}

package com.example.proyectoreciclame.Controller;


import com.example.proyectoreciclame.Dto.UsuarioGestionDto;
import com.example.proyectoreciclame.Dto.UsuarioBloqueoForm;
import com.example.proyectoreciclame.Dto.UsuarioEditForm;

import com.example.proyectoreciclame.Entity.HistorialRoles;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Entity.Rol;
import com.example.proyectoreciclame.Entity.UsuarioEmpresa;

import com.example.proyectoreciclame.Repository.*;

import com.example.proyectoreciclame.util.PaginationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Optional;

import java.util.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;

@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final HistorialRolesRepository historialRolesRepository;
    private final IntentoLoginRepository intentoLoginRepository;

    public AdminUsuarioController(UsuarioRepository usuarioRepository,
                                  RolRepository rolRepository,
                                  UsuarioEmpresaRepository usuarioEmpresaRepository,
                                  HistorialRolesRepository historialRolesRepository,
                                  IntentoLoginRepository intentoLoginRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.historialRolesRepository = historialRolesRepository;
        this.intentoLoginRepository = intentoLoginRepository;
    }

    @GetMapping("/gestion")
    public String gestionUsuarios(@RequestParam(required = false) String texto,
                                  @RequestParam(required = false) List<String> rol,
                                  @RequestParam(required = false) String dateStart,
                                  @RequestParam(required = false) String dateEnd,
                                  @RequestParam(required = false) List<String> estado,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(required = false) String modal,
                                  @RequestParam(required = false) Long id,
                                  Model model) {

        Pageable pageable = PageRequest.of(page, 3);
        Page<Usuario> paginaUsuarios;

        boolean hasRoles = rol != null && !rol.isEmpty();
        if (hasRoles) {
            rol = rol.stream().map(String::toUpperCase).toList();
        }
        boolean hasEstados = estado != null && !estado.isEmpty();

        LocalDateTime fechaInicio = null;
        LocalDateTime fechaFin = null;
        if (dateStart != null && !dateStart.isEmpty()) {
            fechaInicio = java.time.LocalDate.parse(dateStart).atStartOfDay();
        }
        if (dateEnd != null && !dateEnd.isEmpty()) {
            fechaFin = java.time.LocalDate.parse(dateEnd).atTime(23, 59, 59);
        }

        if ((texto == null || texto.isBlank()) && !hasRoles && !hasEstados && fechaInicio == null && fechaFin == null) {
            paginaUsuarios = usuarioRepository.findAllGestionUsuarios(pageable);
        } else {
            paginaUsuarios = usuarioRepository.buscarEnGestionAvanzado(texto, hasRoles, rol, fechaInicio, fechaFin, hasEstados, estado, pageable);
        }

        List<UsuarioGestionDto> lista = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        for (Usuario u : paginaUsuarios.getContent()) {
            String nombreCompleto = construirNombreCompleto(
                    u.getNombres(),
                    u.getApellidoPaterno(),
                    u.getApellidoMaterno()
            );

            String empresa = (u.getUsuarioEmpresa() != null && u.getUsuarioEmpresa().getRazonSocial() != null)
                    ? u.getUsuarioEmpresa().getRazonSocial()
                    : "Sin empresa";

            String rolUsuario = (u.getRol() != null) ? u.getRol().getNombre() : "Sin rol";
            String estadoUsuario = obtenerEstadoVisible(u);
            String fecha = (u.getFechaRegistro() != null) ? u.getFechaRegistro().format(formatter) : "-";
            String iniciales = obtenerIniciales(u.getNombres(), u.getApellidoPaterno());

            lista.add(new UsuarioGestionDto(
                    u.getIdUsuario(),
                    nombreCompleto,
                    u.getCorreo(),
                    u.getDni(),
                    empresa,
                    rolUsuario,
                    estadoUsuario,
                    fecha,
                    iniciales
            ));
        }

        model.addAttribute("usuarios", lista);
        model.addAttribute("texto", texto);
        model.addAttribute("selectedRoles", rol);
        model.addAttribute("dateStart", dateStart);
        model.addAttribute("dateEnd", dateEnd);
        model.addAttribute("selectedEstados", estado);
        model.addAttribute("currentSection", "admin-usuarios");

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paginaUsuarios.getTotalPages());
        model.addAttribute("hasPrevious", paginaUsuarios.hasPrevious());
        model.addAttribute("hasNext", paginaUsuarios.hasNext());

        model.addAttribute("totalUsuarios", usuarioRepository.countByEliminadoEnIsNull());
        model.addAttribute("usuariosActivos", usuarioRepository.countByEstadoCuentaAndEliminadoEnIsNull(Usuario.EstadoCuenta.ACTIVO));
        model.addAttribute("usuariosBloqueados", usuarioRepository.countByEstadoCuentaAndEliminadoEnIsNull(Usuario.EstadoCuenta.BLOQUEADO));
        model.addAttribute("usuariosPendientes", usuarioRepository.countByEstadoAprobacionAndEliminadoEnIsNull("PENDIENTE"));

        model.addAttribute(
                "pageNumbers",
                PaginationUtils.buildPageNumbers(page, paginaUsuarios.getTotalPages())
        );

        model.addAttribute("modal", modal);

        if ("edit".equals(modal) && id != null) {
            cargarModalEditar(id, model);
        }

        if ("block".equals(modal) && id != null) {
            cargarModalBloqueo(id, model, true);
        }

        if ("unblock".equals(modal) && id != null) {
            cargarModalBloqueo(id, model, false);
        }

        // ── NUEVO: modal historial ────────────────────────────────────────────
        if ("historial".equals(modal) && id != null) {
            cargarModalHistorial(id, model);
        }
        // ──────────────────────────────────────────────────────────────────────

        return "admin/gestion-usuarios";
    }

    @GetMapping("/gestion/exportar")
    public void exportarGestionUsuarios(@RequestParam(required = false) String texto,
                                        @RequestParam(required = false) List<String> rol,
                                        @RequestParam(required = false) String dateStart,
                                        @RequestParam(required = false) String dateEnd,
                                        @RequestParam(required = false) List<String> estado,
                                        HttpServletResponse response) throws IOException {

        boolean hasRoles = rol != null && !rol.isEmpty();
        if (hasRoles) {
            rol = rol.stream().map(String::toUpperCase).toList();
        }

        boolean hasEstados = estado != null && !estado.isEmpty();

        LocalDateTime fechaInicio = null;
        LocalDateTime fechaFin = null;

        if (dateStart != null && !dateStart.isEmpty()) {
            fechaInicio = java.time.LocalDate.parse(dateStart).atStartOfDay();
        }

        if (dateEnd != null && !dateEnd.isEmpty()) {
            fechaFin = java.time.LocalDate.parse(dateEnd).atTime(23, 59, 59);
        }

        Page<Usuario> paginaUsuarios;

        if ((texto == null || texto.isBlank()) && !hasRoles && !hasEstados && fechaInicio == null && fechaFin == null) {
            paginaUsuarios = usuarioRepository.findAllGestionUsuarios(Pageable.unpaged());
        } else {
            paginaUsuarios = usuarioRepository.buscarEnGestionAvanzado(
                    texto, hasRoles, rol, fechaInicio, fechaFin, hasEstados, estado, Pageable.unpaged()
            );
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Usuarios");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row header = sheet.createRow(0);

        String[] columnas = {
                "ID", "Nombres", "Apellido paterno", "Apellido materno",
                "DNI", "Correo", "Teléfono", "Empresa", "Rol",
                "Estado aprobación", "Estado cuenta", "Fecha registro"
        };

        for (int i = 0; i < columnas.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;

        for (Usuario u : paginaUsuarios.getContent()) {
            Row row = sheet.createRow(rowIndex++);

            String empresa = (u.getUsuarioEmpresa() != null && u.getUsuarioEmpresa().getRazonSocial() != null)
                    ? u.getUsuarioEmpresa().getRazonSocial() : "-";

            String rolUsuario = u.getRol() != null && u.getRol().getNombre() != null
                    ? u.getRol().getNombre() : "-";

            String estadoCuenta = u.getEstadoCuenta() != null ? u.getEstadoCuenta().name() : "-";
            String fechaRegistro = u.getFechaRegistro() != null ? u.getFechaRegistro().format(formatter) : "-";

            row.createCell(0).setCellValue(u.getIdUsuario() != null ? u.getIdUsuario() : 0);
            row.createCell(1).setCellValue(u.getNombres() != null ? u.getNombres() : "-");
            row.createCell(2).setCellValue(u.getApellidoPaterno() != null ? u.getApellidoPaterno() : "-");
            row.createCell(3).setCellValue(u.getApellidoMaterno() != null ? u.getApellidoMaterno() : "-");
            row.createCell(4).setCellValue(u.getDni() != null ? u.getDni() : "-");
            row.createCell(5).setCellValue(u.getCorreo() != null ? u.getCorreo() : "-");
            row.createCell(6).setCellValue(u.getTelefono() != null ? u.getTelefono() : "-");
            row.createCell(7).setCellValue(empresa);
            row.createCell(8).setCellValue(rolUsuario);
            row.createCell(9).setCellValue(u.getEstadoAprobacion() != null ? u.getEstadoAprobacion() : "-");
            row.createCell(10).setCellValue(estadoCuenta);
            row.createCell(11).setCellValue(fechaRegistro);
        }

        for (int i = 0; i < columnas.length; i++) {
            sheet.autoSizeColumn(i);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=usuarios_reciclame.xlsx");

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @PostMapping("/editar")
    public String editarUsuario(@ModelAttribute("editForm") UsuarioEditForm form,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(required = false) String texto,
                                @RequestParam(required = false) List<String> rol,
                                @RequestParam(required = false) String dateStart,
                                @RequestParam(required = false) String dateEnd,
                                @RequestParam(required = false) List<String> estado,
                                Authentication authentication,   // ← agrega esto
                                Model model) {

        Usuario usuario = usuarioRepository.findById(form.getIdUsuario()).orElse(null);
        if (usuario == null) {
            return "redirect:/admin/usuarios/gestion?page=" + page;
        }

        Map<String, String> errores = validarFormularioEdicion(form);
        if (!errores.isEmpty()) {
            model.addAttribute("fieldErrors", errores);
            model.addAttribute("modal", "edit");
            model.addAttribute("editForm", form);
            model.addAttribute("roles", rolRepository.findAll());
            cargarVistaGestionBase(texto, rol, dateStart, dateEnd, estado, page, model);
            return "admin/gestion-usuarios";
        }

        // ── Guardar el rol anterior ANTES de modificar ────────────────────────
        Rol rolAnterior = usuario.getRol();
        // ─────────────────────────────────────────────────────────────────────

        usuario.setNombres(form.getNombres().trim());
        usuario.setApellidoPaterno(form.getApellidoPaterno().trim());
        usuario.setApellidoMaterno(form.getApellidoMaterno() != null ? form.getApellidoMaterno().trim() : null);
        usuario.setDni(form.getDni().trim());
        usuario.setCorreo(form.getCorreo().trim());
        usuario.setTelefono(form.getTelefono().trim());
        usuario.setActualizadoEn(LocalDateTime.now());

        Rol rolNuevo = null;
        if (form.getIdRol() != null) {
            rolNuevo = rolRepository.findById(form.getIdRol()).orElse(null);
            if (rolNuevo != null) {
                usuario.setRol(rolNuevo);
            }
        }

        if ("RECHAZADO".equalsIgnoreCase(usuario.getEstadoAprobacion())
                && "PENDIENTE".equalsIgnoreCase(form.getEstadoAprobacion())) {
            usuario.setEstadoAprobacion("PENDIENTE");
            usuario.setEstadoCuenta(null);
        }

        usuarioRepository.save(usuario);

        // ── Guardar en historial SOLO si el rol cambió ────────────────────────
        boolean rolCambio = rolNuevo != null
                && (rolAnterior == null || !rolAnterior.getIdRol().equals(rolNuevo.getIdRol()));

        if (rolCambio) {
            Usuario adminQueActua = usuarioRepository
                    .findByCorreoWithRol(authentication.getName()).orElse(null);

            HistorialRoles historial = new HistorialRoles();
            historial.setUsuarioAfectado(usuario);
            historial.setRolAnterior(rolAnterior);
            historial.setRolNuevo(rolNuevo);
            historial.setEstadoAnterior(usuario.getEstadoCuenta());
            historial.setEstadoNuevo(usuario.getEstadoCuenta());
            historial.setAutorizadoPor(adminQueActua);
            historial.setMotivo("Cambio de rol por administrador");
            historial.setFechaCambio(LocalDateTime.now());
            historialRolesRepository.save(historial);
        }
        // ─────────────────────────────────────────────────────────────────────

        Optional<UsuarioEmpresa> usuarioEmpresaOpt = usuarioEmpresaRepository
                .findByUsuario_IdUsuario(usuario.getIdUsuario());
        if (usuarioEmpresaOpt.isPresent()) {
            UsuarioEmpresa ue = usuarioEmpresaOpt.get();
            ue.setRazonSocial(form.getRazonSocial() != null ? form.getRazonSocial().trim() : null);
            ue.setCargo(form.getCargo() != null ? form.getCargo().trim() : null);
            usuarioEmpresaRepository.save(ue);
        }

        String url = "redirect:/admin/usuarios/gestion?page=" + page;
        if (texto != null && !texto.isBlank()) url += "&texto=" + texto;
        return url;
    }

    @PostMapping("/bloquear")
    public String bloquearUsuario(@ModelAttribute("blockForm") UsuarioBloqueoForm form,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(required = false) String texto,
                                  @RequestParam(required = false) List<String> rol,
                                  @RequestParam(required = false) String dateStart,
                                  @RequestParam(required = false) String dateEnd,
                                  @RequestParam(required = false) List<String> estado,
                                  Authentication authentication) {

        Usuario usuario = usuarioRepository.findById(form.getIdUsuario()).orElse(null);
        if (usuario == null) {
            return "redirect:/admin/usuarios/gestion?page=" + page;
        }

        Usuario.EstadoCuenta estadoAnterior = usuario.getEstadoCuenta();

        usuario.setEstadoCuenta(Usuario.EstadoCuenta.BLOQUEADO);
        usuario.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // ── NUEVO: guardar en historial ───────────────────────────────────────
        Usuario adminQueActua = usuarioRepository
                .findByCorreoWithRol(authentication.getName()).orElse(null);

        HistorialRoles historial = new HistorialRoles();
        historial.setUsuarioAfectado(usuario);
        historial.setRolAnterior(usuario.getRol());
        historial.setRolNuevo(usuario.getRol());
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(Usuario.EstadoCuenta.BLOQUEADO);
        historial.setAutorizadoPor(adminQueActua);
        historial.setMotivo(form.getMotivo() != null ? form.getMotivo() : "Cuenta bloqueada por administrador");
        historialRolesRepository.save(historial);
        // ──────────────────────────────────────────────────────────────────────

        String url = "redirect:/admin/usuarios/gestion?page=" + page;
        if (texto != null && !texto.isBlank()) url += "&texto=" + texto;
        return url;
    }

    @PostMapping("/desbloquear")
    public String desbloquearUsuario(@ModelAttribute("blockForm") UsuarioBloqueoForm form,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(required = false) String texto,
                                     @RequestParam(required = false) List<String> rol,
                                     @RequestParam(required = false) String dateStart,
                                     @RequestParam(required = false) String dateEnd,
                                     @RequestParam(required = false) List<String> estado,
                                     Authentication authentication) {

        Usuario usuario = usuarioRepository.findById(form.getIdUsuario()).orElse(null);
        if (usuario == null) {
            return "redirect:/admin/usuarios/gestion?page=" + page;
        }

        usuario.setEstadoCuenta(Usuario.EstadoCuenta.ACTIVO);
        usuario.setEstadoAprobacion("APROBADO");
        usuario.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(usuario);

        LocalDateTime desde = LocalDateTime.now().minusMinutes(15);
        intentoLoginRepository.eliminarIntentosFallidosRecientes(
                usuario.getCorreo(), desde);

        // ── NUEVO: guardar en historial ───────────────────────────────────────
        Usuario adminQueActua = usuarioRepository
                .findByCorreoWithRol(authentication.getName()).orElse(null);

        HistorialRoles historial = new HistorialRoles();
        historial.setUsuarioAfectado(usuario);
        historial.setRolAnterior(usuario.getRol());
        historial.setRolNuevo(usuario.getRol());
        historial.setEstadoAnterior(Usuario.EstadoCuenta.BLOQUEADO);
        historial.setEstadoNuevo(Usuario.EstadoCuenta.ACTIVO);
        historial.setAutorizadoPor(adminQueActua);
        historial.setMotivo("Cuenta desbloqueada por administrador");
        historialRolesRepository.save(historial);
        // ──────────────────────────────────────────────────────────────────────

        String url = "redirect:/admin/usuarios/gestion?page=" + page;
        if (texto != null && !texto.isBlank()) url += "&texto=" + texto;
        return url;
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private void cargarModalEditar(Long id, Model model) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) return;

        UsuarioEditForm form = new UsuarioEditForm();
        form.setIdUsuario(usuario.getIdUsuario());
        form.setNombres(usuario.getNombres());
        form.setApellidoPaterno(usuario.getApellidoPaterno());
        form.setApellidoMaterno(usuario.getApellidoMaterno());
        form.setDni(usuario.getDni());
        form.setCorreo(usuario.getCorreo());
        form.setTelefono(usuario.getTelefono());
        form.setIdRol(usuario.getRol() != null ? usuario.getRol().getIdRol() : null);
        form.setEstadoAprobacion(usuario.getEstadoAprobacion());

        if (usuario.getUsuarioEmpresa() != null) {
            form.setRazonSocial(usuario.getUsuarioEmpresa().getRazonSocial());
            form.setCargo(usuario.getUsuarioEmpresa().getCargo());
        }

        model.addAttribute("editForm", form);
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("mostrarSelectorPendiente",
                "RECHAZADO".equalsIgnoreCase(usuario.getEstadoAprobacion()));
    }

    private void cargarModalBloqueo(Long id, Model model, boolean bloquear) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) return;

        UsuarioBloqueoForm form = new UsuarioBloqueoForm();
        form.setIdUsuario(usuario.getIdUsuario());

        model.addAttribute("blockForm", form);
        model.addAttribute("usuarioBloqueoNombre",
                construirNombreCompleto(usuario.getNombres(), usuario.getApellidoPaterno(), usuario.getApellidoMaterno()));
        model.addAttribute("usuarioBloqueoCorreo", usuario.getCorreo());
        model.addAttribute("modoBloqueo", bloquear ? "bloquear" : "desbloquear");
    }

    // ── NUEVO: cargar datos para el modal historial ───────────────────────────
    private void cargarModalHistorial(Long id, Model model) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) return;

        List<HistorialRoles> historial =
                historialRolesRepository.findByUsuarioAfectadoOrderByFechaCambioDesc(usuario);

        model.addAttribute("usuarioHistorial", usuario);
        model.addAttribute("historialRoles", historial);
    }
    // ──────────────────────────────────────────────────────────────────────────

    private void cargarVistaGestionBase(String texto, List<String> rol, String dateStart, String dateEnd,
                                        List<String> estado, int page, Model model) {
        Pageable pageable = PageRequest.of(page, 3);
        Page<Usuario> paginaUsuarios;

        boolean hasRoles = rol != null && !rol.isEmpty();
        if (hasRoles) rol = rol.stream().map(String::toUpperCase).toList();
        boolean hasEstados = estado != null && !estado.isEmpty();

        LocalDateTime fechaInicio = null;
        LocalDateTime fechaFin = null;
        if (dateStart != null && !dateStart.isEmpty()) fechaInicio = java.time.LocalDate.parse(dateStart).atStartOfDay();
        if (dateEnd != null && !dateEnd.isEmpty()) fechaFin = java.time.LocalDate.parse(dateEnd).atTime(23, 59, 59);

        if ((texto == null || texto.isBlank()) && !hasRoles && !hasEstados && fechaInicio == null && fechaFin == null) {
            paginaUsuarios = usuarioRepository.findAllGestionUsuarios(pageable);
        } else {
            paginaUsuarios = usuarioRepository.buscarEnGestionAvanzado(texto, hasRoles, rol, fechaInicio, fechaFin, hasEstados, estado, pageable);
        }

        List<UsuarioGestionDto> lista = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        for (Usuario u : paginaUsuarios.getContent()) {
            String nombreCompleto = construirNombreCompleto(u.getNombres(), u.getApellidoPaterno(), u.getApellidoMaterno());
            String empresa = (u.getUsuarioEmpresa() != null && u.getUsuarioEmpresa().getRazonSocial() != null)
                    ? u.getUsuarioEmpresa().getRazonSocial() : "Sin empresa";
            String rolUsuario = (u.getRol() != null) ? u.getRol().getNombre() : "Sin rol";
            String estadoUsuario = obtenerEstadoVisible(u);
            String fecha = (u.getFechaRegistro() != null) ? u.getFechaRegistro().format(formatter) : "-";
            String iniciales = obtenerIniciales(u.getNombres(), u.getApellidoPaterno());

            lista.add(new UsuarioGestionDto(u.getIdUsuario(), nombreCompleto, u.getCorreo(), u.getDni(),
                    empresa, rolUsuario, estadoUsuario, fecha, iniciales));
        }

        model.addAttribute("usuarios", lista);
        model.addAttribute("texto", texto);
        model.addAttribute("selectedRoles", rol);
        model.addAttribute("dateStart", dateStart);
        model.addAttribute("dateEnd", dateEnd);
        model.addAttribute("selectedEstados", estado);
        model.addAttribute("currentSection", "admin-usuarios");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paginaUsuarios.getTotalPages());
        model.addAttribute("hasPrevious", paginaUsuarios.hasPrevious());
        model.addAttribute("hasNext", paginaUsuarios.hasNext());
        model.addAttribute("pageNumbers", PaginationUtils.buildPageNumbers(page, paginaUsuarios.getTotalPages()));
        model.addAttribute("totalUsuarios", usuarioRepository.countByEliminadoEnIsNull());
        model.addAttribute("usuariosActivos", usuarioRepository.countByEstadoCuentaAndEliminadoEnIsNull(Usuario.EstadoCuenta.ACTIVO));
        model.addAttribute("usuariosBloqueados", usuarioRepository.countByEstadoCuentaAndEliminadoEnIsNull(Usuario.EstadoCuenta.BLOQUEADO));
        model.addAttribute("usuariosPendientes", usuarioRepository.countByEstadoAprobacionAndEliminadoEnIsNull("PENDIENTE"));
    }

    private Map<String, String> validarFormularioEdicion(UsuarioEditForm form) {
        Map<String, String> errores = new HashMap<>();
        if (form.getNombres() == null || form.getNombres().isBlank()) errores.put("nombres", "Campo nombre obligatorio");
        if (form.getApellidoPaterno() == null || form.getApellidoPaterno().isBlank()) errores.put("apellidoPaterno", "Campo apellido paterno obligatorio");
        if (form.getDni() == null || form.getDni().isBlank()) errores.put("dni", "Campo DNI obligatorio");
        else if (!form.getDni().matches("\\d{8}")) errores.put("dni", "El DNI debe tener 8 dígitos");
        if (form.getCorreo() == null || form.getCorreo().isBlank()) errores.put("correo", "Campo correo obligatorio");
        else if (!form.getCorreo().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) errores.put("correo", "Correo no válido");
        if (form.getTelefono() == null || form.getTelefono().isBlank()) errores.put("telefono", "Campo teléfono obligatorio");
        if (form.getIdRol() == null) errores.put("idRol", "Debe seleccionar un rol");
        return errores;
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

    private String obtenerEstadoVisible(Usuario u) {
        if (u.getEstadoCuenta() == Usuario.EstadoCuenta.BLOQUEADO) return "Bloqueado";
        if ("PENDIENTE".equalsIgnoreCase(u.getEstadoAprobacion())) return "Pendiente";
        if ("RECHAZADO".equalsIgnoreCase(u.getEstadoAprobacion())) return "Rechazado";
        if (u.getEstadoCuenta() == Usuario.EstadoCuenta.ACTIVO) return "Activo";
        if ("APROBADO".equalsIgnoreCase(u.getEstadoAprobacion())) return "Activo";
        return "Sin estado";
    }
}

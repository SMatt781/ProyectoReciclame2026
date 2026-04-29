package com.example.proyectoreciclame.Controller;


import com.example.proyectoreciclame.Dto.UsuarioGestionDto;
import com.example.proyectoreciclame.Dto.UsuarioBloqueoForm;
import com.example.proyectoreciclame.Dto.UsuarioEditForm;

import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Entity.Rol;
import com.example.proyectoreciclame.Entity.UsuarioEmpresa;

import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Repository.RolRepository;
import com.example.proyectoreciclame.Repository.UsuarioEmpresaRepository;

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

@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;

    public AdminUsuarioController(UsuarioRepository usuarioRepository,
                                  RolRepository rolRepository,
                                  UsuarioEmpresaRepository usuarioEmpresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
    }

    @GetMapping("/gestion")
    public String gestionUsuarios(@RequestParam(required = false) String texto,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(required = false) String modal,
                                  @RequestParam(required = false) Long id,
                                  Model model) {


        Pageable pageable = PageRequest.of(page, 3);
        Page<Usuario> paginaUsuarios;

        if (texto == null || texto.isBlank()) {
            paginaUsuarios = usuarioRepository.findAllGestionUsuarios(pageable);
        } else {
            paginaUsuarios = usuarioRepository.buscarEnGestion(texto, pageable);
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

            String rol = (u.getRol() != null) ? u.getRol().getNombre() : "Sin rol";
            String estado = obtenerEstadoVisible(u);
            String fecha = (u.getFechaRegistro() != null) ? u.getFechaRegistro().format(formatter) : "-";
            String iniciales = obtenerIniciales(u.getNombres(), u.getApellidoPaterno());

            lista.add(new UsuarioGestionDto(
                    u.getIdUsuario(),
                    nombreCompleto,
                    u.getCorreo(),
                    u.getDni(),
                    empresa,
                    rol,
                    estado,
                    fecha,
                    iniciales
            ));
        }

        model.addAttribute("usuarios", lista);
        model.addAttribute("texto", texto);
        model.addAttribute("currentSection", "admin-usuarios");

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paginaUsuarios.getTotalPages());
        model.addAttribute("hasPrevious", paginaUsuarios.hasPrevious());
        model.addAttribute("hasNext", paginaUsuarios.hasNext());

        model.addAttribute("totalUsuarios", usuarioRepository.countByEliminadoEnIsNull());
        model.addAttribute("usuariosActivos", usuarioRepository.countByEstadoCuentaAndEliminadoEnIsNull(Usuario.EstadoCuenta.ACTIVO));
        model.addAttribute("usuariosBloqueados", usuarioRepository.countByEstadoCuentaAndEliminadoEnIsNull(Usuario.EstadoCuenta.BLOQUEADO));
        model.addAttribute("usuariosPendientes", usuarioRepository.countByEstadoAprobacionAndEliminadoEnIsNull("PENDIENTE"));

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

        return "admin/gestion-usuarios";
    }

    @PostMapping("/editar")
    public String editarUsuario(@ModelAttribute("editForm") UsuarioEditForm form,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(required = false) String texto,
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

            cargarVistaGestionBase(texto, page, model);
            return "admin/gestion-usuarios";
        }

        usuario.setNombres(form.getNombres().trim());
        usuario.setApellidoPaterno(form.getApellidoPaterno().trim());
        usuario.setApellidoMaterno(form.getApellidoMaterno() != null ? form.getApellidoMaterno().trim() : null);
        usuario.setDni(form.getDni().trim());
        usuario.setCorreo(form.getCorreo().trim());
        usuario.setTelefono(form.getTelefono().trim());
        usuario.setActualizadoEn(LocalDateTime.now());

        if (form.getIdRol() != null) {
            Rol rol = rolRepository.findById(form.getIdRol()).orElse(null);
            if (rol != null) {
                usuario.setRol(rol);
            }
        }

        // Solo permitir volver de RECHAZADO a PENDIENTE
        if ("RECHAZADO".equalsIgnoreCase(usuario.getEstadoAprobacion())
                && "PENDIENTE".equalsIgnoreCase(form.getEstadoAprobacion())) {
            usuario.setEstadoAprobacion("PENDIENTE");
            usuario.setEstadoCuenta(null);
        }

        usuarioRepository.save(usuario);

        Optional<UsuarioEmpresa> usuarioEmpresaOpt = usuarioEmpresaRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        if (usuarioEmpresaOpt.isPresent()) {
            UsuarioEmpresa ue = usuarioEmpresaOpt.get();
            ue.setRazonSocial(form.getRazonSocial() != null ? form.getRazonSocial().trim() : null);
            ue.setCargo(form.getCargo() != null ? form.getCargo().trim() : null);
            usuarioEmpresaRepository.save(ue);
        }

        String url = "redirect:/admin/usuarios/gestion?page=" + page;
        if (texto != null && !texto.isBlank()) {
            url += "&texto=" + texto;
        }
        return url;
    }

    @PostMapping("/bloquear")
    public String bloquearUsuario(@ModelAttribute("blockForm") UsuarioBloqueoForm form,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(required = false) String texto) {

        Usuario usuario = usuarioRepository.findById(form.getIdUsuario()).orElse(null);
        if (usuario == null) {
            return "redirect:/admin/usuarios/gestion?page=" + page;
        }

        usuario.setEstadoCuenta(Usuario.EstadoCuenta.BLOQUEADO);
        usuario.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String url = "redirect:/admin/usuarios/gestion?page=" + page;
        if (texto != null && !texto.isBlank()) {
            url += "&texto=" + texto;
        }
        return url;
    }

    @PostMapping("/desbloquear")
    public String desbloquearUsuario(@ModelAttribute("blockForm") UsuarioBloqueoForm form,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(required = false) String texto) {

        Usuario usuario = usuarioRepository.findById(form.getIdUsuario()).orElse(null);
        if (usuario == null) {
            return "redirect:/admin/usuarios/gestion?page=" + page;
        }

        usuario.setEstadoCuenta(Usuario.EstadoCuenta.ACTIVO);
        usuario.setEstadoAprobacion("APROBADO");
        usuario.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String url = "redirect:/admin/usuarios/gestion?page=" + page;
        if (texto != null && !texto.isBlank()) {
            url += "&texto=" + texto;
        }
        return url;
    }

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

    private void cargarVistaGestionBase(String texto, int page, Model model) {
        Pageable pageable = PageRequest.of(page, 3);
        Page<Usuario> paginaUsuarios;

        if (texto == null || texto.isBlank()) {
            paginaUsuarios = usuarioRepository.findAllGestionUsuarios(pageable);
        } else {
            paginaUsuarios = usuarioRepository.buscarEnGestion(texto, pageable);
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

            String rol = (u.getRol() != null) ? u.getRol().getNombre() : "Sin rol";
            String estado = obtenerEstadoVisible(u);
            String fecha = (u.getFechaRegistro() != null) ? u.getFechaRegistro().format(formatter) : "-";
            String iniciales = obtenerIniciales(u.getNombres(), u.getApellidoPaterno());

            lista.add(new UsuarioGestionDto(
                    u.getIdUsuario(),
                    nombreCompleto,
                    u.getCorreo(),
                    u.getDni(),
                    empresa,
                    rol,
                    estado,
                    fecha,
                    iniciales
            ));
        }

        model.addAttribute("usuarios", lista);
        model.addAttribute("texto", texto);
        model.addAttribute("currentSection", "admin-usuarios");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paginaUsuarios.getTotalPages());
        model.addAttribute("hasPrevious", paginaUsuarios.hasPrevious());
        model.addAttribute("hasNext", paginaUsuarios.hasNext());

        model.addAttribute("totalUsuarios", usuarioRepository.countByEliminadoEnIsNull());
        model.addAttribute("usuariosActivos", usuarioRepository.countByEstadoCuentaAndEliminadoEnIsNull(Usuario.EstadoCuenta.ACTIVO));
        model.addAttribute("usuariosBloqueados", usuarioRepository.countByEstadoCuentaAndEliminadoEnIsNull(Usuario.EstadoCuenta.BLOQUEADO));
        model.addAttribute("usuariosPendientes", usuarioRepository.countByEstadoAprobacionAndEliminadoEnIsNull("PENDIENTE"));
    }

    private Map<String, String> validarFormularioEdicion(UsuarioEditForm form) {
        Map<String, String> errores = new HashMap<>();

        if (form.getNombres() == null || form.getNombres().isBlank()) {
            errores.put("nombres", "Campo nombre obligatorio");
        }

        if (form.getApellidoPaterno() == null || form.getApellidoPaterno().isBlank()) {
            errores.put("apellidoPaterno", "Campo apellido paterno obligatorio");
        }

        if (form.getDni() == null || form.getDni().isBlank()) {
            errores.put("dni", "Campo DNI obligatorio");
        } else if (!form.getDni().matches("\\d{8}")) {
            errores.put("dni", "El DNI debe tener 8 dígitos");
        }

        if (form.getCorreo() == null || form.getCorreo().isBlank()) {
            errores.put("correo", "Campo correo obligatorio");
        } else if (!form.getCorreo().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            errores.put("correo", "Correo no válido");
        }

        if (form.getTelefono() == null || form.getTelefono().isBlank()) {
            errores.put("telefono", "Campo teléfono obligatorio");
        }

        if (form.getIdRol() == null) {
            errores.put("idRol", "Debe seleccionar un rol");
        }

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


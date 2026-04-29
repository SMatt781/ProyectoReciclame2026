package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Entity.DominioAutorizado;
import com.example.proyectoreciclame.Entity.Rol;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.DominioAutorizadoRepository;
import com.example.proyectoreciclame.Repository.RolRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/superadmin")
public class SuperadminController {

    // ── Constantes ────────────────────────────────────────────────────────────
    private static final List<Integer> ROL_ADMIN_IDS = List.of(2);
    private static final int PAGE_SIZE = 3;

    // ── Dependencias ─────────────────────────────────────────────────────────
    final UsuarioRepository usuarioRepository;
    final DominioAutorizadoRepository dominioAutorizadoRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;

    public SuperadminController(UsuarioRepository usuarioRepository,
                                DominioAutorizadoRepository dominioAutorizadoRepository, BCryptPasswordEncoder passwordEncoder, RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.dominioAutorizadoRepository = dominioAutorizadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        // Puedes añadir algún dato aquí si quieres
        model.addAttribute("titulo", "Dashboard");
        model.addAttribute("currentSection", "superadmin-dashboard");
        return "superadmin/dashboard"; // Este es el nombre del archivo HTML de tu vista
    }

    // Nuevo método para mostrar administradores
    // ── Administradores — GET ────────────────────────────────────────────────

    @GetMapping("/administradores")
    public String showAdministradores(
            Model model,
            @RequestParam(value = "texto", required = false) String texto,
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        PageRequest pageable = PageRequest.of(
                page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "idUsuario"));

        Page<Usuario> administradores;
        if (texto != null && !texto.isBlank()) {
            administradores = usuarioRepository
                    .buscarEnGestion(texto, ROL_ADMIN_IDS, pageable);
        } else {
            administradores = usuarioRepository
                    .findByRol_IdInAndEliminadoEnIsNull(ROL_ADMIN_IDS, pageable);
        }

        Map<Long, String> ultimoAccesoTexto = administradores.getContent().stream()
                .collect(Collectors.toMap(
                        Usuario::getIdUsuario,
                        admin -> formatearUltimoAcceso(admin.getUltimoAcceso())
                ));

        model.addAttribute("ultimoAccesoTexto", ultimoAccesoTexto);
        model.addAttribute("titulo", "Administradores");
        model.addAttribute("currentSection", "superadmin-administradores");
        model.addAttribute("administradores", administradores);
        model.addAttribute("totalAdmins",
                usuarioRepository.countByRol_IdInAndEliminadoEnIsNull(ROL_ADMIN_IDS));
        model.addAttribute("activeAdmins",
                usuarioRepository.countActiveAdminsByRole(ROL_ADMIN_IDS));
        model.addAttribute("blockedAdmins",
                usuarioRepository.countBlockedAdminsByRole(ROL_ADMIN_IDS));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", administradores.getTotalPages());
        model.addAttribute("hasPrevious", administradores.hasPrevious());
        model.addAttribute("hasNext", administradores.hasNext());
        model.addAttribute("texto", texto);

        return "superadmin/administradores";
    }

    // ── Administradores — POST (Crear) ───────────────────────────────────────

    @PostMapping("/administradores/crear")
    public String crearAdministrador(
            @ModelAttribute Usuario usuario,
            RedirectAttributes redirectAttributes
    ) {
        // 1. Validar unicidad de correo
        if (usuarioRepository.existsByCorreoAndEliminadoEnIsNull(usuario.getCorreo())) {
            redirectAttributes.addFlashAttribute("error",
                    "Ya existe un administrador con ese correo electrónico.");
            return "redirect:/superadmin/administradores";
        }

        // 2. Validar unicidad de DNI
        if (usuario.getDni() != null && !usuario.getDni().isBlank()
                && usuarioRepository.existsByDniAndEliminadoEnIsNull(usuario.getDni())) {
            redirectAttributes.addFlashAttribute("error",
                    "Ya existe un administrador con ese DNI.");
            return "redirect:/superadmin/administradores";
        }

        // 3. Encriptar contraseña
        usuario.setContrasenaHash(passwordEncoder.encode(usuario.getContrasenaHash()));

        // 4. Asignar Rol ID = 2 (Administrador)
        Rol rolAdmin = rolRepository.findById(2)
                .orElseThrow(() -> new IllegalStateException(
                        "Rol 'Administrador' (id=2) no encontrado en la base de datos."));
        usuario.setRol(rolAdmin);

        // 5. Estado de cuenta activo usando el Enum interno
        usuario.setEstadoCuenta(Usuario.EstadoCuenta.ACTIVO);
        usuario.setEstadoAprobacion("APROBADO");  // ← AGREGA ESTA LÍNEA
        // 6. Guardar
        usuarioRepository.save(usuario);

        redirectAttributes.addFlashAttribute("success",
                "Administrador creado exitosamente.");
        return "redirect:/superadmin/administradores";
    }

    // ── Administradores — GET (cargar datos para editar) ─────────────────────────

    @GetMapping("/administradores/editar/{id}")
    public String editarAdministradorForm(
            @PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "texto", required = false) String texto,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Usuario admin = usuarioRepository.findById(id).orElse(null);
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "Administrador no encontrado.");
            return "redirect:/superadmin/administradores";
        }

        // Recargar la lista para mostrar la página con el modal abierto
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "idUsuario"));
        Page<Usuario> administradores = (texto != null && !texto.isBlank())
                ? usuarioRepository.buscarEnGestion(texto, ROL_ADMIN_IDS, pageable)
                : usuarioRepository.findByRol_IdInAndEliminadoEnIsNull(ROL_ADMIN_IDS, pageable);

        Map<Long, String> ultimoAccesoTexto = administradores.getContent().stream()
                .collect(Collectors.toMap(
                        Usuario::getIdUsuario,
                        a -> formatearUltimoAcceso(a.getUltimoAcceso())
                ));

        model.addAttribute("ultimoAccesoTexto", ultimoAccesoTexto);
        model.addAttribute("administradores", administradores);
        model.addAttribute("totalAdmins", usuarioRepository.countByRol_IdInAndEliminadoEnIsNull(ROL_ADMIN_IDS));
        model.addAttribute("activeAdmins", usuarioRepository.countActiveAdminsByRole(ROL_ADMIN_IDS));
        model.addAttribute("blockedAdmins", usuarioRepository.countBlockedAdminsByRole(ROL_ADMIN_IDS));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", administradores.getTotalPages());
        model.addAttribute("hasPrevious", administradores.hasPrevious());
        model.addAttribute("hasNext", administradores.hasNext());
        model.addAttribute("texto", texto);
        model.addAttribute("titulo", "Administradores");
        model.addAttribute("currentSection", "superadmin-administradores");

        // Datos del admin a editar — van al modal
        model.addAttribute("adminEditar", admin);
        model.addAttribute("modalEditar", true);

        return "superadmin/administradores";
    }

// ── Administradores — POST (guardar edición) ─────────────────────────────────

    @PostMapping("/administradores/editar")
    public String editarAdministrador(
            @RequestParam Long idUsuario,
            @RequestParam String nombres,
            @RequestParam String apellidoPaterno,
            @RequestParam(required = false) String apellidoMaterno,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String telefono,
            @RequestParam String correo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "texto", required = false) String texto,
            RedirectAttributes redirectAttributes
    ) {
        Usuario admin = usuarioRepository.findById(idUsuario).orElse(null);
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "Administrador no encontrado.");
            return "redirect:/superadmin/administradores";
        }

        // Validar unicidad de correo (excluyendo el mismo usuario)
        if (!admin.getCorreo().equalsIgnoreCase(correo)
                && usuarioRepository.existsByCorreoAndEliminadoEnIsNull(correo)) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un administrador con ese correo.");
            return "redirect:/superadmin/administradores?page=" + page
                    + (texto != null ? "&texto=" + texto : "");
        }

        // Validar unicidad de DNI (excluyendo el mismo usuario)
        if (dni != null && !dni.isBlank()
                && !dni.equals(admin.getDni())
                && usuarioRepository.existsByDniAndEliminadoEnIsNull(dni)) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un administrador con ese DNI.");
            return "redirect:/superadmin/administradores?page=" + page
                    + (texto != null ? "&texto=" + texto : "");
        }

        admin.setNombres(nombres.trim());
        admin.setApellidoPaterno(apellidoPaterno.trim());
        admin.setApellidoMaterno(apellidoMaterno != null ? apellidoMaterno.trim() : null);
        admin.setDni(dni != null ? dni.trim() : null);
        admin.setTelefono(telefono != null ? telefono.trim() : null);
        admin.setCorreo(correo.trim());
        admin.setActualizadoEn(LocalDateTime.now());

        usuarioRepository.save(admin);

        redirectAttributes.addFlashAttribute("success", "Administrador actualizado correctamente.");
        return "redirect:/superadmin/administradores?page=" + page
                + (texto != null ? "&texto=" + texto : "");
    }

// ── Administradores — POST (bloquear / desbloquear) ──────────────────────────

    @PostMapping("/administradores/bloquear")
    public String bloquearAdministrador(
            @RequestParam Long idUsuario,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "texto", required = false) String texto,
            RedirectAttributes redirectAttributes
    ) {
        Usuario admin = usuarioRepository.findById(idUsuario).orElse(null);
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "Administrador no encontrado.");
            return "redirect:/superadmin/administradores";
        }

        boolean estaBloqueado = admin.getEstadoCuenta() == Usuario.EstadoCuenta.BLOQUEADO;

        if (estaBloqueado) {
            admin.setEstadoCuenta(Usuario.EstadoCuenta.ACTIVO);
            redirectAttributes.addFlashAttribute("success", "Administrador desbloqueado correctamente.");
        } else {
            admin.setEstadoCuenta(Usuario.EstadoCuenta.BLOQUEADO);
            redirectAttributes.addFlashAttribute("success", "Administrador bloqueado correctamente.");
        }

        admin.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(admin);

        return "redirect:/superadmin/administradores?page=" + page
                + (texto != null ? "&texto=" + texto : "");
    }


    private String formatearUltimoAcceso(LocalDateTime ultimoAcceso) {
        if (ultimoAcceso == null) {
            return "Sin registro";
        }

        Duration duracion = Duration.between(ultimoAcceso, LocalDateTime.now());

        long minutos = duracion.toMinutes();
        long horas = duracion.toHours();
        long dias = duracion.toDays();

        if (minutos < 1) {
            return "Hace unos segundos";
        } else if (minutos < 60) {
            return "Hace " + minutos + (minutos == 1 ? " minuto" : " minutos");
        } else if (horas < 24) {
            return "Hace " + horas + (horas == 1 ? " hora" : " horas");
        } else {
            return "Hace " + dias + (dias == 1 ? " día" : " días");
        }
    }



    // Nuevo método para el Estado del Monitor
    @GetMapping("/estadoSistema")
    public String showEstadoSistema(Model model) {
        model.addAttribute("titulo", "Estado del Sistema");
        model.addAttribute("currentSection", "superadmin-estado-sistema");
        // Aquí podrías agregar más lógica si necesitas información adicional
        return "superadmin/estadoSistema"; // Vista del estado del sistema
    }

    // Nuevo método para la Configuración de Seguridad
    @GetMapping("/confSeguridad")
    public String showConfSeguridad(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(value = "texto", required = false) String texto
    ) {
        model.addAttribute("titulo", "Configuración de Seguridad");
        model.addAttribute("currentSection", "superadmin-conf-seguridad");

        PageRequest pageable = PageRequest.of(page, 3, Sort.by("fechaRegistro").descending());
        Page<DominioAutorizado> dominiosPage;

        if (texto != null && !texto.trim().isEmpty()) {
            dominiosPage = dominioAutorizadoRepository
                    .findByNombreDominioContainingIgnoreCase(texto.trim(), pageable);
        } else {
            dominiosPage = dominioAutorizadoRepository.findAll(pageable);
        }

        model.addAttribute("dominios", dominiosPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", dominiosPage.getTotalPages());
        model.addAttribute("hasPrevious", dominiosPage.hasPrevious());
        model.addAttribute("hasNext", dominiosPage.hasNext());
        model.addAttribute("texto", texto);

        return "superadmin/confSeguridad";
    }
    // ── Dominios — POST (Crear) ───────────────────────────────────────────────────

    @PostMapping("/confSeguridad/crear")
    public String crearDominio(
            @RequestParam String nombreDominio,
            @RequestParam(required = false) String motivoAutorizacion,
            @RequestParam(value = "page", defaultValue = "0") int page,
            RedirectAttributes redirectAttributes,
            org.springframework.security.core.Authentication authentication
    ) {
        // Normalizar: asegurarse que empiece con @
        String dominio = nombreDominio.trim();
        if (!dominio.startsWith("@")) {
            dominio = "@" + dominio;
        }

        if (dominioAutorizadoRepository.existsByNombreDominioIgnoreCase(dominio)) {
            redirectAttributes.addFlashAttribute("error", "El dominio '" + dominio + "' ya está registrado.");
            return "redirect:/superadmin/confSeguridad?page=" + page;
        }

        // Obtener el usuario autenticado como creador
        Usuario creador = usuarioRepository.findByCorreoWithRol(authentication.getName()).orElse(null);

        DominioAutorizado nuevo = new DominioAutorizado();
        nuevo.setNombreDominio(dominio);
        nuevo.setMotivoAutorizacion(motivoAutorizacion != null ? motivoAutorizacion.trim() : null);
        nuevo.setEstado(true);
        nuevo.setUsuarioCreador(creador);

        nuevo.setFechaRegistro(java.time.LocalDateTime.now());

        dominioAutorizadoRepository.save(nuevo);

        redirectAttributes.addFlashAttribute("success", "Dominio '" + dominio + "' añadido correctamente.");
        return "redirect:/superadmin/confSeguridad?page=" + page;
    }

// ── Dominios — GET (cargar datos para editar) ────────────────────────────────

    @GetMapping("/confSeguridad/editar/{id}")
    public String editarDominioForm(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(value = "texto", required = false) String texto,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        DominioAutorizado dominio = dominioAutorizadoRepository.findById(id).orElse(null);
        if (dominio == null) {
            redirectAttributes.addFlashAttribute("error", "Dominio no encontrado.");
            return "redirect:/superadmin/confSeguridad";
        }

        // Recargar la lista
        PageRequest pageable = PageRequest.of(page, 3, Sort.by("fechaRegistro").descending());
        Page<DominioAutorizado> dominiosPage = (texto != null && !texto.isBlank())
                ? dominioAutorizadoRepository.findByNombreDominioContainingIgnoreCase(texto.trim(), pageable)
                : dominioAutorizadoRepository.findAll(pageable);

        model.addAttribute("dominios", dominiosPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", dominiosPage.getTotalPages());
        model.addAttribute("hasPrevious", dominiosPage.hasPrevious());
        model.addAttribute("hasNext", dominiosPage.hasNext());
        model.addAttribute("texto", texto);
        model.addAttribute("titulo", "Configuración de Seguridad");
        model.addAttribute("currentSection", "superadmin-conf-seguridad");

        // Datos del dominio a editar
        model.addAttribute("dominioEditar", dominio);
        model.addAttribute("modalEditarDominio", true);

        return "superadmin/confSeguridad";
    }

// ── Dominios — POST (Editar) ─────────────────────────────────────────────────

    @PostMapping("/confSeguridad/editar")
    public String editarDominio(
            @RequestParam Integer idDominio,
            @RequestParam String nombreDominio,
            @RequestParam(required = false) String motivoAutorizacion,
            @RequestParam(required = false) Boolean estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(value = "texto", required = false) String texto,
            RedirectAttributes redirectAttributes
    ) {
        DominioAutorizado dominio = dominioAutorizadoRepository.findById(idDominio).orElse(null);
        if (dominio == null) {
            redirectAttributes.addFlashAttribute("error", "Dominio no encontrado.");
            return "redirect:/superadmin/confSeguridad";
        }

        String nuevoNombre = nombreDominio.trim();
        if (!nuevoNombre.startsWith("@")) {
            nuevoNombre = "@" + nuevoNombre;
        }

        // Validar unicidad excluyendo el mismo registro
        if (!dominio.getNombreDominio().equalsIgnoreCase(nuevoNombre)
                && dominioAutorizadoRepository.existsByNombreDominioIgnoreCase(nuevoNombre)) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un dominio con ese nombre.");
            return "redirect:/superadmin/confSeguridad?page=" + page
                    + (texto != null ? "&texto=" + texto : "");
        }

        dominio.setNombreDominio(nuevoNombre);
        dominio.setMotivoAutorizacion(motivoAutorizacion != null ? motivoAutorizacion.trim() : null);
        dominio.setEstado(estado != null ? estado : dominio.getEstado());

        dominioAutorizadoRepository.save(dominio);

        redirectAttributes.addFlashAttribute("success", "Dominio actualizado correctamente.");
        return "redirect:/superadmin/confSeguridad?page=" + page
                + (texto != null ? "&texto=" + texto : "");
    }

// ── Dominios — POST (Eliminar) ───────────────────────────────────────────────

    @PostMapping("/confSeguridad/eliminar")
    public String eliminarDominio(
            @RequestParam Integer idDominio,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(value = "texto", required = false) String texto,
            RedirectAttributes redirectAttributes
    ) {
        if (!dominioAutorizadoRepository.existsById(idDominio)) {
            redirectAttributes.addFlashAttribute("error", "Dominio no encontrado.");
            return "redirect:/superadmin/confSeguridad";
        }

        dominioAutorizadoRepository.deleteById(idDominio);

        redirectAttributes.addFlashAttribute("success", "Dominio eliminado correctamente.");
        return "redirect:/superadmin/confSeguridad?page=" + page
                + (texto != null ? "&texto=" + texto : "");
    }

    // ── Dominios — POST (Cambiar estado) ─────────────────────────────────────────

    @PostMapping("/confSeguridad/cambiarEstado")
    public String cambiarEstadoDominio(
            @RequestParam Integer idDominio,
            @RequestParam Boolean nuevoEstado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(value = "texto", required = false) String texto,
            RedirectAttributes redirectAttributes
    ) {
        DominioAutorizado dominio = dominioAutorizadoRepository.findById(idDominio).orElse(null);
        if (dominio == null) {
            redirectAttributes.addFlashAttribute("error", "Dominio no encontrado.");
            return "redirect:/superadmin/confSeguridad";
        }

        dominio.setEstado(nuevoEstado);
        dominioAutorizadoRepository.save(dominio);

        String accion = nuevoEstado ? "activado" : "desactivado";
        redirectAttributes.addFlashAttribute("success",
                "Dominio '" + dominio.getNombreDominio() + "' " + accion + " correctamente.");

        return "redirect:/superadmin/confSeguridad?page=" + page
                + (texto != null ? "&texto=" + texto : "");
    }

}

package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Entity.*;
import com.example.proyectoreciclame.Repository.*;
import com.example.proyectoreciclame.Service.CorreoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final PoliticaContrasenaRepository politicaContrasenaRepository;
    private final NormativaRepository normativaRepository;
    private final EstudioRepository estudioRepository;
    private final NotificacionRepository notificacionRepository;
    private final RegistroSesionRepository registroSesionRepository;
    private final IntentoLoginRepository intentoLoginRepository;
    private final RecuperacionPasswordRepository recuperacionPasswordRepository;
    private final SolicitudRegistroRepository solicitudRegistroRepository;
    private final org.springframework.core.env.Environment env;
    private final CorreoService correoService;

    public SuperadminController(UsuarioRepository usuarioRepository,
                                DominioAutorizadoRepository dominioAutorizadoRepository,
                                BCryptPasswordEncoder passwordEncoder,
                                RolRepository rolRepository,
                                PoliticaContrasenaRepository politicaContrasenaRepository,
                                NormativaRepository normativaRepository,
                                EstudioRepository estudioRepository,
                                NotificacionRepository notificacionRepository,
                                RegistroSesionRepository registroSesionRepository,
                                IntentoLoginRepository intentoLoginRepository,
                                RecuperacionPasswordRepository recuperacionPasswordRepository,
                                SolicitudRegistroRepository solicitudRegistroRepository,
                                CorreoService correoService,
                                org.springframework.core.env.Environment env) {
        this.usuarioRepository = usuarioRepository;
        this.dominioAutorizadoRepository = dominioAutorizadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.politicaContrasenaRepository = politicaContrasenaRepository;
        this.normativaRepository = normativaRepository;
        this.estudioRepository = estudioRepository;
        this.notificacionRepository = notificacionRepository;
        this.registroSesionRepository = registroSesionRepository;
        this.intentoLoginRepository = intentoLoginRepository;
        this.recuperacionPasswordRepository = recuperacionPasswordRepository;
        this.solicitudRegistroRepository = solicitudRegistroRepository;
        this.correoService = correoService;
        this.env = env;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("titulo", "Dashboard");
        model.addAttribute("currentSection", "superadmin-dashboard");

        // Métricas reales
        model.addAttribute("totalAdmins",
                usuarioRepository.countByRol_IdInAndEliminadoEnIsNull(ROL_ADMIN_IDS));
        model.addAttribute("activeAdmins",
                usuarioRepository.countActiveAdminsByRole(ROL_ADMIN_IDS));
        model.addAttribute("totalDominios",
                dominioAutorizadoRepository.countByEstadoTrue()); // agregar al repo
        model.addAttribute("politica",
                politicaContrasenaRepository.findById(1).orElse(null));

        // Últimos 3 admins (para la tabla reciente)
        PageRequest ultimos = PageRequest.of(0, 3,
                Sort.by(Sort.Direction.DESC, "idUsuario"));
        model.addAttribute("ultimosAdmins",
                usuarioRepository.findByRol_IdInAndEliminadoEnIsNull(ROL_ADMIN_IDS, ultimos));

        return "superadmin/dashboard";
    }

    // Nuevo método para mostrar administradores
    // ── Administradores — GET ────────────────────────────────────────────────

    @GetMapping("/administradores")
    public String showAdministradores(
            Model model,
            @RequestParam(value = "texto", required = false) String texto,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        PageRequest pageable = PageRequest.of(
                page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "idUsuario"));

        Page<Usuario> administradores;

        if (texto != null && !texto.isBlank() && estado != null && !estado.isBlank()) {
            // Búsqueda + filtro de estado combinados
            administradores = usuarioRepository
                    .buscarEnGestionConEstado(texto, ROL_ADMIN_IDS,
                            Usuario.EstadoCuenta.valueOf(estado), pageable);
        } else if (texto != null && !texto.isBlank()) {
            administradores = usuarioRepository
                    .buscarEnGestion(texto, ROL_ADMIN_IDS, pageable);
        } else if (estado != null && !estado.isBlank()) {
            administradores = usuarioRepository
                    .findByRolIdInAndEstadoCuentaAndEliminadoEnIsNull(
                            ROL_ADMIN_IDS, Usuario.EstadoCuenta.valueOf(estado), pageable);
        } else {
            administradores = usuarioRepository
                    .findByRol_IdInAndEliminadoEnIsNull(ROL_ADMIN_IDS, pageable);
        }

        Map<Long, String> ultimoAccesoTexto = administradores.getContent().stream()
                .collect(Collectors.toMap(
                        Usuario::getIdUsuario,
                        admin -> formatearUltimoAcceso(admin.getUltimoAcceso())
                ));
        model.addAttribute("estado", estado);
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
        model.addAttribute("listaDominios",
                dominioAutorizadoRepository.findByEstadoTrue());
        model.addAttribute("politica",
                politicaContrasenaRepository.findById(1).orElse(null));
        model.addAttribute("ultimoAdmin",
                usuarioRepository.findUltimoAdminCreado(ROL_ADMIN_IDS).orElse(null));
        return "superadmin/administradores";
    }

    // ── Administradores — POST (Crear) ───────────────────────────────────────

    @PostMapping("/administradores/crear")
    public String crearAdministrador(
            @ModelAttribute Usuario usuario,
            @RequestParam String usuarioCorreo,
            @RequestParam String dominioCorreo,
            RedirectAttributes redirectAttributes
    ) {
        // Construir correo completo — dominioCorreo ya tiene el @
        String correoCompleto = usuarioCorreo.trim() + dominioCorreo.trim();
        usuario.setCorreo(correoCompleto);

        // 1. Validar unicidad de correo
        if (usuarioRepository.existsByCorreo(correoCompleto)) {
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
        // 3. Validar contraseña contra políticas activas
        String rawPassword = usuario.getContrasenaHash();
        PoliticaContrasena politica = politicaContrasenaRepository.findById(1).orElse(null);

        if (politica != null) {
            List<String> erroresPolitica = new ArrayList<>();

            if (rawPassword.length() < politica.getLongitudMinima()) {
                erroresPolitica.add("mínimo " + politica.getLongitudMinima() + " caracteres");
            }
            if (politica.getRequiereMayuscula() && !rawPassword.matches(".*[A-Z].*")) {
                erroresPolitica.add("al menos una mayúscula");
            }
            if (politica.getRequiereNumero() && !rawPassword.matches(".*[0-9].*")) {
                erroresPolitica.add("al menos un número");
            }
            if (politica.getRequiereSimbolo() && !rawPassword.matches(".*[@#$!|*%&].*")) {
                erroresPolitica.add("al menos un símbolo (@#$!|*%&)");
            }

            if (!erroresPolitica.isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "La contraseña no cumple las políticas: " + String.join(", ", erroresPolitica) + ".");
                return "redirect:/superadmin/administradores";
            }
        }

// 4. Encriptar contraseña
        usuario.setContrasenaHash(passwordEncoder.encode(rawPassword));
        // 4. Asignar Rol ID = 2 (Administrador)
        Rol rolAdmin = rolRepository.findById(2)
                .orElseThrow(() -> new IllegalStateException(
                        "Rol 'Administrador' (id=2) no encontrado en la base de datos."));
        usuario.setRol(rolAdmin);

        // 5. Estado de cuenta activo
        usuario.setEstadoCuenta(Usuario.EstadoCuenta.ACTIVO);
        usuario.setEstadoAprobacion("APROBADO");

        // 6. Guardar
        usuarioRepository.save(usuario);

        // Enviar correo con credenciales al nuevo administrador
        try {
            correoService.enviarCredencialesAdministrador(
                    correoCompleto,
                    usuario.getNombres(),
                    rawPassword
            );
        } catch (Exception e) {
            System.out.println("ERROR al enviar correo: " + e.getMessage());
        }

        redirectAttributes.addFlashAttribute("success",
                "Administrador creado exitosamente. Se envió un correo con las credenciales a " + correoCompleto + ".");

        crearNotificacionSuperadmin(
                "Nuevo administrador creado",
                "Se creó el administrador " + correoCompleto + " correctamente.",
                "ADMIN_CREADO",
                "/superadmin/administradores"
        );
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

        model.addAttribute("adminEditar", admin);
        model.addAttribute("modalEditar", true);
        model.addAttribute("listaDominios",
                dominioAutorizadoRepository.findByEstadoTrue());



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
            @RequestParam String usuarioCorreo,
            @RequestParam String dominioCorreo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "texto", required = false) String texto,
            RedirectAttributes redirectAttributes
    ) {
        // Construir correo completo
        String correo = usuarioCorreo.trim() + dominioCorreo.trim();

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
        admin.setCorreo(correo);
        admin.setActualizadoEn(LocalDateTime.now());

        usuarioRepository.save(admin);

        redirectAttributes.addFlashAttribute("success", "Administrador actualizado correctamente.");

        crearNotificacionSuperadmin(
                "Administrador editado",
                "Se actualizaron los datos de " + nombres + " " + apellidoPaterno + ".",
                "ADMIN_EDITADO",
                "/superadmin/administradores"
        );

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

        crearNotificacionSuperadmin(
                estaBloqueado ? "Administrador desbloqueado" : "Administrador bloqueado",
                (estaBloqueado ? "Se desbloqueó" : "Se bloqueó") + " la cuenta de " + admin.getNombres() + " " + admin.getApellidoPaterno(),
                "ADMIN_BLOQUEADO",
                "/superadmin/administradores"
        );

        return "redirect:/superadmin/administradores?page=" + page
                + (texto != null ? "&texto=" + texto : "");
    }

    // ── Administradores — POST (Eliminar) ────────────────────────────────────────

    @PostMapping("/administradores/eliminar")
    public String eliminarAdministrador(
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

        // Soft delete — no borramos el registro, solo marcamos eliminadoEn
        admin.setEliminadoEn(LocalDateTime.now());
        admin.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(admin);

        redirectAttributes.addFlashAttribute("success",
                "Administrador eliminado correctamente.");

        crearNotificacionSuperadmin(
                "Administrador eliminado",
                "Se eliminó la cuenta de " + admin.getNombres() + " " + admin.getApellidoPaterno(),
                "ADMIN_ELIMINADO",
                "/superadmin/administradores"
        );

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



    @GetMapping("/estadoSistema")
    public String showEstadoSistema(Model model,
                                    org.springframework.security.core.Authentication authentication) {
        model.addAttribute("titulo", "Estado del Sistema");
        model.addAttribute("currentSection", "superadmin-estado-sistema");

        // ── Conteos de tablas críticas ────────────────────────────────────
        model.addAttribute("totalUsuarios",
                usuarioRepository.countByEliminadoEnIsNull());
        model.addAttribute("totalRoles",
                rolRepository.count());
        model.addAttribute("totalDominios",
                dominioAutorizadoRepository.count());
        model.addAttribute("totalNormativas",
                normativaRepository.count());
        model.addAttribute("totalEstudios",
                estudioRepository.count());
        model.addAttribute("totalNotificaciones",
                notificacionRepository.count());

        // ── Sesiones y seguridad ──────────────────────────────────────────
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        model.addAttribute("sesionesActivas",
                registroSesionRepository.countSesionesActivas());
        model.addAttribute("sesionesHoy",
                registroSesionRepository.countByFechaInicioAfter(inicioDia));
        model.addAttribute("intentosFallidosHoy",
                intentoLoginRepository.countByFechaAfterAndExitosoFalse(inicioDia));
        model.addAttribute("recuperacionesActivas",
                recuperacionPasswordRepository.countByUsadoFalse());
        model.addAttribute("solicitudesPendientes",
                solicitudRegistroRepository.countByEstado("PENDIENTE"));

        // ── Latencia BD ───────────────────────────────────────────────────
        long t0 = System.currentTimeMillis();
        rolRepository.count();
        long latencia = System.currentTimeMillis() - t0;
        model.addAttribute("dbLatencyMs", latencia);
        model.addAttribute("dbConexionOk", true);

        // ── Info de despliegue ────────────────────────────────────────────
        model.addAttribute("appVersion",
                env.getProperty("app.version", "v1.0.0-dev"));
        model.addAttribute("buildEnv",
                env.getProperty("app.env", "LOCAL"));
        model.addAttribute("javaVersion",
                System.getProperty("java.version"));
        model.addAttribute("springVersion",
                org.springframework.core.SpringVersion.getVersion());

        Usuario superadmin = usuarioRepository
                .findByCorreoWithRol(authentication.getName()).orElse(null);
        Long superadminId = superadmin != null ? superadmin.getIdUsuario() : null;
        // ── Alerta automática si hay muchos intentos fallidos ─────────────────
        long intentosFallidos = intentoLoginRepository.countByFechaAfterAndExitosoFalse(inicioDia);
        if (intentosFallidos >= 5 && superadminId != null) {
            boolean yaExisteAlerta = notificacionRepository
                    .existsAlertaSistemaHoy(superadminId, "SISTEMA_ALERTA", inicioDia);
            if (!yaExisteAlerta) {
                crearNotificacionSuperadmin(
                        "⚠️ Alerta de seguridad",
                        "Se detectaron " + intentosFallidos + " intentos de login fallidos hoy. Revisa la actividad del sistema.",
                        "SISTEMA_ALERTA",
                        "/superadmin/estadoSistema"
                );
            }
        }

        return "superadmin/estadoSistema";
    }

    // Nuevo método para la Configuración de Seguridad
    @GetMapping("/confSeguridad")
    public String showConfSeguridad(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(value = "estadoDominio", required = false) String estadoDominio,
            @RequestParam(value = "texto", required = false) String texto
    ) {
        model.addAttribute("titulo", "Configuración de Seguridad");
        model.addAttribute("currentSection", "superadmin-conf-seguridad");
        model.addAttribute("estadoDominio", estadoDominio);

        PageRequest pageable = PageRequest.of(page, 3, Sort.by("fechaRegistro").descending());

        Page<DominioAutorizado> dominiosPage;

        if (texto != null && !texto.isBlank() && estadoDominio != null) {
            boolean activo = estadoDominio.equals("activo");
            dominiosPage = dominioAutorizadoRepository
                    .findByNombreDominioContainingIgnoreCaseAndEstado(texto.trim(), activo, pageable);
        } else if (texto != null && !texto.isBlank()) {
            dominiosPage = dominioAutorizadoRepository
                    .findByNombreDominioContainingIgnoreCase(texto.trim(), pageable);
        } else if (estadoDominio != null) {
            boolean activo = estadoDominio.equals("activo");
            dominiosPage = dominioAutorizadoRepository.findByEstado(activo, pageable);
        } else {
            dominiosPage = dominioAutorizadoRepository.findAll(pageable);
        }

        model.addAttribute("dominios", dominiosPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", dominiosPage.getTotalPages());
        model.addAttribute("hasPrevious", dominiosPage.hasPrevious());
        model.addAttribute("hasNext", dominiosPage.hasNext());
        model.addAttribute("texto", texto);

        PoliticaContrasena politica = politicaContrasenaRepository.findById(1)
                .orElseGet(() -> {
                    PoliticaContrasena nueva = new PoliticaContrasena();
                    nueva.setIdPolitica(1);
                    nueva.setLongitudMinima(8);
                    nueva.setRequiereMayuscula(true);
                    nueva.setRequiereNumero(true);
                    nueva.setRequiereSimbolo(false);
                    nueva.setMandatoMfa(false);
                    nueva.setExpiracionDias(90);
                    return nueva;
                });
        model.addAttribute("politica", politica);

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

        crearNotificacionSuperadmin(
                "Dominio añadido",
                "Se añadió el dominio " + dominio + " correctamente.",
                "DOMINIO_CREADO",
                "/superadmin/confSeguridad"
        );

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

        crearNotificacionSuperadmin(
                "Dominio eliminado",
                "Se eliminó el dominio " + idDominio + " del sistema.",
                "DOMINIO_ELIMINADO",
                "/superadmin/confSeguridad"
        );

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

        crearNotificacionSuperadmin(
                nuevoEstado ? "Dominio activado" : "Dominio desactivado",
                "El dominio " + dominio.getNombreDominio() + " fue " + (nuevoEstado ? "activado" : "desactivado") + ".",
                "DOMINIO_DESACTIVADO",
                "/superadmin/confSeguridad"
        );

        return "redirect:/superadmin/confSeguridad?page=" + page
                + (texto != null ? "&texto=" + texto : "");
    }

    // ── Política de Contraseña — POST (Guardar) ───────────────────────────────────

    @PostMapping("/confSeguridad/politica")
    public String guardarPolitica(
            @RequestParam(defaultValue = "false") Boolean requiereMayuscula,
            @RequestParam(defaultValue = "false") Boolean requiereNumero,
            @RequestParam(defaultValue = "false") Boolean requiereSimbolo,
            @RequestParam(defaultValue = "false") Boolean mandatoMfa,
            @RequestParam Integer longitudMinima,
            @RequestParam(required = false) Integer expiracionDias,
            RedirectAttributes redirectAttributes,
            org.springframework.security.core.Authentication authentication
    ) {
        // Verificar si expiracionDias es null
        if (expiracionDias == null) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar un valor para la expiración de la contraseña.");
            return "redirect:/superadmin/confSeguridad";
        }

        PoliticaContrasena politica = politicaContrasenaRepository.findById(1)
                .orElse(new PoliticaContrasena());

        politica.setIdPolitica(1);
        politica.setRequiereMayuscula(requiereMayuscula);
        politica.setRequiereNumero(requiereNumero);
        politica.setRequiereSimbolo(requiereSimbolo);
        politica.setMandatoMfa(mandatoMfa);
        politica.setLongitudMinima(longitudMinima);
        politica.setExpiracionDias(expiracionDias);

        Usuario actualizador = usuarioRepository
                .findByCorreoWithRol(authentication.getName()).orElse(null);
        politica.setActualizadoPor(actualizador);

        politicaContrasenaRepository.save(politica);

        redirectAttributes.addFlashAttribute("success",
                "Políticas de contraseña actualizadas correctamente.");
        return "redirect:/superadmin/confSeguridad";
    }

    // ── Exportar Administradores a Excel ─────────────────────────────────────
    @GetMapping("/administradores/exportar")
    public void exportarAdministradores(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=administradores.xlsx");

        List<Usuario> admins = usuarioRepository.findByRolIdInAndEliminadoEnIsNull(ROL_ADMIN_IDS);

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {

            org.apache.poi.xssf.usermodel.XSSFSheet sheet =
                    workbook.createSheet("Administradores");

            // Estilos
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle =
                    workbook.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    new org.apache.poi.xssf.usermodel.XSSFColor(
                            new byte[]{(byte)65, (byte)102, (byte)86}, null));
            headerStyle.setFillPattern(
                    org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.xssf.usermodel.XSSFFont whiteFont = workbook.createFont();
            whiteFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            whiteFont.setBold(true);
            headerStyle.setFont(whiteFont);

            // Cabecera
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            String[] cols = {"ID", "Nombres", "Apellido Paterno", "Apellido Materno",
                    "Correo", "DNI", "Teléfono", "Estado", "Último Acceso"};
            for (int i = 0; i < cols.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Datos
            int rowNum = 1;
            for (Usuario admin : admins) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(admin.getIdUsuario());
                row.createCell(1).setCellValue(admin.getNombres());
                row.createCell(2).setCellValue(admin.getApellidoPaterno());
                row.createCell(3).setCellValue(
                        admin.getApellidoMaterno() != null ? admin.getApellidoMaterno() : "");
                row.createCell(4).setCellValue(admin.getCorreo());
                row.createCell(5).setCellValue(
                        admin.getDni() != null ? admin.getDni() : "");
                row.createCell(6).setCellValue(
                        admin.getTelefono() != null ? admin.getTelefono() : "");
                row.createCell(7).setCellValue(admin.getEstadoCuenta().name());
                row.createCell(8).setCellValue(
                        admin.getUltimoAcceso() != null
                                ? admin.getUltimoAcceso().toString() : "Sin registro");
            }

            workbook.write(response.getOutputStream());
        }
    }

    // ── Exportar Dominios a Excel ─────────────────────────────────────────────
    @GetMapping("/confSeguridad/exportar")
    public void exportarDominios(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=dominios.xlsx");

        List<DominioAutorizado> dominios = dominioAutorizadoRepository.findAll();

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {

            org.apache.poi.xssf.usermodel.XSSFSheet sheet =
                    workbook.createSheet("Dominios");

            // Cabecera
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            String[] cols = {"ID", "Dominio", "Estado", "Fecha Registro", "Añadido Por"};
            for (int i = 0; i < cols.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                sheet.setColumnWidth(i, 5000);
            }

            // Datos
            int rowNum = 1;
            for (DominioAutorizado dom : dominios) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dom.getIdDominio());
                row.createCell(1).setCellValue(dom.getNombreDominio());
                row.createCell(2).setCellValue(Boolean.TRUE.equals(dom.getEstado()) ? "ACTIVO" : "INACTIVO");
                row.createCell(3).setCellValue(
                        dom.getFechaRegistro() != null
                                ? dom.getFechaRegistro().toString() : "");
                row.createCell(4).setCellValue(
                        dom.getUsuarioCreador() != null
                                ? dom.getUsuarioCreador().getNombres() + " "
                                + dom.getUsuarioCreador().getApellidoPaterno() : "");
            }

            workbook.write(response.getOutputStream());
        }
    }
    private void crearNotificacionSuperadmin(String titulo, String mensaje,
                                             String tipo, String enlace) {
        try {
            // Buscar al superadmin (rol id = 1)
            List<Integer> rolSuperadmin = List.of(1);
            usuarioRepository.findByRolIdInAndEliminadoEnIsNull(rolSuperadmin)
                    .forEach(superadmin -> {
                        Notificacion n = new Notificacion();
                        n.setUsuario(superadmin);
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

    @GetMapping("/notificaciones")
    public String showNotificaciones(Model model,
                                     org.springframework.security.core.Authentication authentication) {
        Usuario superadmin = usuarioRepository
                .findByCorreoWithRol(authentication.getName()).orElse(null);
        if (superadmin == null) return "redirect:/superadmin/dashboard";

        List<Notificacion> notificaciones = notificacionRepository
                .findByUsuarioIdOrderByFechaDesc(superadmin.getIdUsuario());

        // Formatear tiempo relativo
        LocalDateTime now = LocalDateTime.now();
        List<java.util.Map<String, Object>> notificacionesVm = notificaciones.stream()
                .map(n -> {
                    java.util.Map<String, Object> vm = new java.util.HashMap<>();
                    vm.put("titulo", n.getTitulo());
                    vm.put("mensaje", n.getMensaje());
                    vm.put("tipo", n.getTipo());
                    vm.put("leido", n.getLeido());
                    vm.put("enlace", n.getEnlaceReferencia());
                    vm.put("fecha", n.getFecha());
                    vm.put("id", n.getIdNotificacion());
                    vm.put("tiempo", formatearUltimoAcceso(n.getFecha()));
                    // Etiqueta legible por tipo
                    String etiqueta = switch (n.getTipo() != null ? n.getTipo() : "") {
                        case "ADMIN_CREADO"       -> "Nuevo administrador";
                        case "ADMIN_BLOQUEADO"    -> "Admin bloqueado";
                        case "ADMIN_ELIMINADO"    -> "Admin eliminado";
                        case "DOMINIO_CREADO"     -> "Dominio añadido";
                        case "DOMINIO_DESACTIVADO"-> "Dominio desactivado";
                        case "SOLICITUD"          -> "Solicitud pendiente";
                        case "ADMIN_EDITADO"          -> "Admin editado";
                        case "DOMINIO_ELIMINADO"      -> "Dominio eliminado";
                        case "SISTEMA_ALERTA" -> "Alerta del sistema";
                        default                  -> "Sistema";
                    };
                    vm.put("etiqueta", etiqueta);
                    return vm;
                })
                .toList();

        model.addAttribute("notificaciones", notificacionesVm);
        model.addAttribute("totalNoLeidas",
                notificacionRepository.countNoLeidasByUsuario(superadmin.getIdUsuario()));
        model.addAttribute("titulo", "Notificaciones");
        model.addAttribute("currentSection", "superadmin-notificaciones");
        return "superadmin/notificaciones";
    }

    @PostMapping("/notificaciones/marcar-leidas")
    @Transactional
    public String marcarTodasLeidas(
            org.springframework.security.core.Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Usuario superadmin = usuarioRepository
                .findByCorreoWithRol(authentication.getName()).orElse(null);
        if (superadmin != null) {
            notificacionRepository
                    .findByUsuarioIdOrderByFechaDesc(superadmin.getIdUsuario())
                    .forEach(n -> {
                        n.setLeido(true);
                        notificacionRepository.save(n);
                    });
        }
        redirectAttributes.addFlashAttribute("success", "Todas las notificaciones marcadas como leídas.");
        return "redirect:/superadmin/notificaciones";
    }


    @PostMapping("/notificaciones/marcar-leida/{id}")
    public String marcarLeida(@PathVariable Long id,
                              org.springframework.security.core.Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        Usuario superadmin = usuarioRepository
                .findByCorreoWithRol(authentication.getName()).orElse(null);
        if (superadmin == null) return "redirect:/superadmin/notificaciones";

        notificacionRepository.findById(id).ifPresent(n -> {
            if (n.getUsuario().getIdUsuario().equals(superadmin.getIdUsuario())) {
                n.setLeido(true);
                notificacionRepository.save(n);
            }
        });

        redirectAttributes.addFlashAttribute("success", "Notificación marcada como leída.");
        return "redirect:/superadmin/notificaciones";
    }

}

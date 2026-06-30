package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.RegistroSocioForm;
import com.example.proyectoreciclame.Dto.RegistroVisualizadorForm;
import com.example.proyectoreciclame.Entity.DominioAutorizado;
import com.example.proyectoreciclame.Entity.Identificacion;
import com.example.proyectoreciclame.Entity.PoliticaContrasena;
import com.example.proyectoreciclame.Entity.Rol;
import com.example.proyectoreciclame.Entity.SolicitudRegistro;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Entity.UsuarioEmpresa;
import com.example.proyectoreciclame.Repository.DominioAutorizadoRepository;
import com.example.proyectoreciclame.Repository.IdentificacionRepository;
import com.example.proyectoreciclame.Repository.PoliticaContrasenaRepository;
import com.example.proyectoreciclame.Repository.RolRepository;
import com.example.proyectoreciclame.Repository.SolicitudRegistroRepository;
import com.example.proyectoreciclame.Repository.UsuarioEmpresaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.CorreoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.proyectoreciclame.Repository.RegistroSesionRepository;
import com.example.proyectoreciclame.Entity.RegistroSesion;
import java.util.List;


import java.time.LocalDateTime;

@Controller
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final SolicitudRegistroRepository solicitudRegistroRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CorreoService correoService;
    private final DominioAutorizadoRepository dominioAutorizadoRepository;
    private final PoliticaContrasenaRepository politicaContrasenaRepository;
    private final RegistroSesionRepository registroSesionRepository;
    private final AdminNotificacionController adminNotificacionController;
    private final IdentificacionRepository identificacionRepository;
    private final com.example.proyectoreciclame.Service.NotificacionWebSocketService webSocketService;

    public AuthController(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          UsuarioEmpresaRepository usuarioEmpresaRepository,
                          SolicitudRegistroRepository solicitudRegistroRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          CorreoService correoService,
                          DominioAutorizadoRepository dominioAutorizadoRepository,
                          PoliticaContrasenaRepository politicaContrasenaRepository,
                          RegistroSesionRepository registroSesionRepository,
                          AdminNotificacionController adminNotificacionController,
                          IdentificacionRepository identificacionRepository,
                          com.example.proyectoreciclame.Service.NotificacionWebSocketService webSocketService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.solicitudRegistroRepository = solicitudRegistroRepository;
        this.passwordEncoder = passwordEncoder;
        this.correoService = correoService;
        this.dominioAutorizadoRepository = dominioAutorizadoRepository;
        this.politicaContrasenaRepository = politicaContrasenaRepository;
        this.registroSesionRepository = registroSesionRepository;
        this.adminNotificacionController = adminNotificacionController;
        this.identificacionRepository = identificacionRepository;
        this.webSocketService = webSocketService;
    }

    @Value("${google.recaptcha.site-key}")
    private String recaptchaSiteKey;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String correo,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) Long restantes,
                        Model model) {

        model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
        model.addAttribute("correoIngresado", correo);

        // Tipo de error para la vista
        if ("bloqueado".equals(error)) {
            model.addAttribute("errorBloqueado", true);
        } else if ("intentos".equals(error) && restantes != null) {
            model.addAttribute("errorIntentos", true);
            model.addAttribute("intentosRestantes", restantes);
        } else if ("true".equals(error)) {
            model.addAttribute("errorCredenciales", true);
        }

        return "auth/login";
    }

    @GetMapping("/post-login")
    public String postLogin(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<RegistroSesion> sesionesVigentes =
                registroSesionRepository.findByUsuarioAndEstado(usuario, "VIGENTE");

        for (RegistroSesion s : sesionesVigentes) {
            s.setEstado("FINALIZADA");
            s.setFechaFin(LocalDateTime.now());
        }
        registroSesionRepository.saveAll(sesionesVigentes);

        RegistroSesion nuevaSesion = new RegistroSesion();
        nuevaSesion.setUsuario(usuario);
        nuevaSesion.setFechaInicio(LocalDateTime.now());
        nuevaSesion.setEstado("VIGENTE");
        registroSesionRepository.save(nuevaSesion);
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"))) {
            return "redirect:/superadmin/dashboard";
        }

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/superadmin/dashboard";
        }

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SOCIO"))) {
            return "redirect:/socio/home";
        }

        return "redirect:/visualizador/home";
    }

    @GetMapping("/registro/socio")
    public String registroSocio(Model model) {
        model.addAttribute("registroForm", new RegistroSocioForm());
        cargarPoliticaPasswordEnModelo(model);
        return "auth/registro-socio";
    }

    @PostMapping("/registro/socio")
    public String registrarSocio(@Valid @ModelAttribute("registroForm") RegistroSocioForm form,
                                 BindingResult bindingResult,
                                 Model model) {

        validarRegistroSocio(form, bindingResult);

        if (bindingResult.hasErrors()) {
            cargarPoliticaPasswordEnModelo(model);
            return "auth/registro-socio";
        }

        Rol rolSocio = rolRepository.findByNombreIgnoreCase("SOCIO")
                .orElseThrow(() -> new RuntimeException("Rol SOCIO no existe"));

        Usuario usuario = new Usuario();
        usuario.setRol(rolSocio);
        usuario.setCorreo(form.getCorreo().trim().toLowerCase());
        usuario.setTelefono(form.getTelefono().trim());
        usuario.setContrasenaHash(passwordEncoder.encode(form.getPassword()));
        usuario.setEstadoAprobacion("PENDIENTE");
        usuario.setEstadoCuenta(null);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setActualizadoEn(LocalDateTime.now());
        usuario.setUltimoAcceso(null);
        usuario.setEliminadoEn(null);

        // Manejo diferente según tipo de documento
        if ("DNI".equalsIgnoreCase(form.getTipoIdentificacion())) {
            // DNI: completar nombres y apellidos en usuario
            usuario.setNombres(form.getNombres().trim());
            usuario.setApellidoPaterno(form.getApellidoPaterno() != null ? form.getApellidoPaterno().trim() : null);
            usuario.setApellidoMaterno(form.getApellidoMaterno() != null ? form.getApellidoMaterno().trim() : null);
        } else {
            // RUC: usar nombres para referencia, dejar apellidos NULL
            usuario.setNombres(form.getNombres().trim()); // Será la razón social
            usuario.setApellidoPaterno(null);
            usuario.setApellidoMaterno(null);
        }

        usuarioRepository.save(usuario);

        // Guardar identificacion (DNI o RUC)
        Identificacion identificacion = new Identificacion();
        identificacion.setUsuario(usuario);
        identificacion.setTipo(Identificacion.TipoIdentificacion.valueOf(form.getTipoIdentificacion().toUpperCase()));
        identificacion.setNumero(form.getNumeroIdentificacion().trim());
        identificacionRepository.save(identificacion);

        // Si es RUC crear también usuario_empresa con razón social verificada por RENIEC
        if ("RUC".equalsIgnoreCase(form.getTipoIdentificacion())) {
            UsuarioEmpresa ue = new UsuarioEmpresa();
            ue.setUsuario(usuario);
            // form.getNombres() contiene la razón social obtenida de RENIEC/SUNAT
            ue.setRazonSocial(form.getNombres().trim());
            ue.setCargo(null);
            usuarioEmpresaRepository.save(ue);
        }

        // Construir nombre completo para la solicitud
        String nombreCompleto = usuario.getNombres();
        if (usuario.getApellidoPaterno() != null) {
            nombreCompleto += " " + usuario.getApellidoPaterno();
        }
        if (usuario.getApellidoMaterno() != null) {
            nombreCompleto += " " + usuario.getApellidoMaterno();
        }

        SolicitudRegistro solicitud = new SolicitudRegistro();
        solicitud.setNombres(usuario.getNombres());
        solicitud.setApellidoPaterno(usuario.getApellidoPaterno());
        solicitud.setApellidoMaterno(usuario.getApellidoMaterno());
        solicitud.setTipoIdentificacion(form.getTipoIdentificacion().toUpperCase());
        solicitud.setNumeroIdentificacion(form.getNumeroIdentificacion().trim());
        solicitud.setCorreo(usuario.getCorreo());
        solicitud.setTelefono(usuario.getTelefono());
        solicitud.setRolSolicitado("SOCIO");
        solicitud.setEstado("PENDIENTE");
        solicitud.setMotivoRechazo(null);
        solicitud.setRevisadoPor(null);
        solicitud.setIdUsuarioCreado(usuario.getIdUsuario());
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setFechaResolucion(null);

        solicitudRegistroRepository.save(solicitud);
        long pendientes = usuarioRepository.countByEstadoAprobacionAndEliminadoEnIsNull("PENDIENTE");
        webSocketService.enviarTopico("/topic/admin/solicitudes", java.util.Map.of("count", pendientes));

        correoService.enviarConfirmacionRegistro(
                usuario.getCorreo(),
                usuario.getNombres(),
                "SOCIO"
        );

        // Crear notificación para los admins
        adminNotificacionController.crearNotificacionAdmin(
                "Nueva solicitud de registro - Socio",
                nombreCompleto + " ha solicitado registrarse como SOCIO. Correo: " + usuario.getCorreo(),
                "REGISTRO",
                "/admin/usuarios/solicitudes"
        );

        return "redirect:/solicitud-enviada";
    }

    @GetMapping("/registro/visualizador")
    public String registroVisualizador(Model model) {
        model.addAttribute("registroForm", new RegistroVisualizadorForm());
        cargarPoliticaPasswordEnModelo(model);
        return "auth/registro-visualizador";
    }

    @PostMapping("/registro/visualizador")
    public String registrarVisualizador(@Valid @ModelAttribute("registroForm") RegistroVisualizadorForm form,
                                        BindingResult bindingResult,
                                        Model model) {

        validarRegistroVisualizador(form, bindingResult);

        if (bindingResult.hasErrors()) {
            cargarPoliticaPasswordEnModelo(model);
            return "auth/registro-visualizador";
        }

        Rol rolVisualizador = rolRepository.findByNombreIgnoreCase("VISUALIZADOR")
                .orElseThrow(() -> new RuntimeException("Rol VISUALIZADOR no existe"));

        Usuario usuario = new Usuario();
        usuario.setRol(rolVisualizador);
        usuario.setNombres(form.getNombres().trim());
        usuario.setApellidoPaterno(form.getApellidoPaterno().trim());
        usuario.setApellidoMaterno(form.getApellidoMaterno().trim());
        usuario.setCorreo(form.getCorreo().trim().toLowerCase());
        usuario.setTelefono(form.getTelefono().trim());
        usuario.setContrasenaHash(passwordEncoder.encode(form.getPassword()));
        usuario.setEstadoAprobacion("PENDIENTE");
        usuario.setEstadoCuenta(null);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setActualizadoEn(LocalDateTime.now());
        usuario.setUltimoAcceso(null);
        usuario.setEliminadoEn(null);

        usuarioRepository.save(usuario);

        // Visualizador solo puede tener DNI
        Identificacion identificacion = new Identificacion();
        identificacion.setUsuario(usuario);
        identificacion.setTipo(Identificacion.TipoIdentificacion.DNI);
        identificacion.setNumero(form.getNumeroIdentificacion().trim());
        identificacionRepository.save(identificacion);

        SolicitudRegistro solicitud = new SolicitudRegistro();
        solicitud.setNombres(usuario.getNombres());
        solicitud.setApellidoPaterno(usuario.getApellidoPaterno());
        solicitud.setApellidoMaterno(usuario.getApellidoMaterno());
        solicitud.setTipoIdentificacion("DNI");
        solicitud.setNumeroIdentificacion(form.getNumeroIdentificacion().trim());
        solicitud.setCorreo(usuario.getCorreo());
        solicitud.setTelefono(usuario.getTelefono());
        solicitud.setRolSolicitado("VISUALIZADOR");
        solicitud.setEstado("PENDIENTE");
        solicitud.setMotivoRechazo(null);
        solicitud.setRevisadoPor(null);
        solicitud.setIdUsuarioCreado(usuario.getIdUsuario());
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setFechaResolucion(null);

        solicitudRegistroRepository.save(solicitud);
        long pendientesVis = usuarioRepository.countByEstadoAprobacionAndEliminadoEnIsNull("PENDIENTE");
        webSocketService.enviarTopico("/topic/admin/solicitudes", java.util.Map.of("count", pendientesVis));

        correoService.enviarConfirmacionRegistro(
                usuario.getCorreo(),
                usuario.getNombres(),
                "VISUALIZADOR"
        );

        // Crear notificación para los admins
        adminNotificacionController.crearNotificacionAdmin(
                "Nueva solicitud de registro - Visualizador",
                usuario.getNombres() + " " + usuario.getApellidoPaterno() + " ha solicitado registrarse como VISUALIZADOR. Correo: " + usuario.getCorreo(),
                "REGISTRO",
                "/admin/usuarios/solicitudes"
        );

        return "redirect:/solicitud-enviada";
    }

    @GetMapping("/solicitud-enviada")
    public String solicitudEnviada() {
        return "auth/solicitud-enviada";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    private void validarRegistroSocio(RegistroSocioForm form, BindingResult br) {
        // Validar apellidos según tipo de documento
        if ("DNI".equalsIgnoreCase(form.getTipoIdentificacion())) {
            // DNI: apellidos son obligatorios
            if (form.getApellidoPaterno() == null || form.getApellidoPaterno().isBlank()) {
                br.rejectValue("apellidoPaterno", "apellidoPaterno.required", "Campo apellido paterno obligatorio");
            }
            if (form.getApellidoMaterno() == null || form.getApellidoMaterno().isBlank()) {
                br.rejectValue("apellidoMaterno", "apellidoMaterno.required", "Campo apellido materno obligatorio");
            }
        }
        // RUC: apellidos son opcionales (NULL), no validar

        if (form.getCorreo() != null && !form.getCorreo().isBlank()
                && usuarioRepository.existsByCorreoIgnoreCase(form.getCorreo().trim())) {
            br.rejectValue("correo", "correo.exists", "El correo ya está registrado");
        }

        if (form.getNumeroIdentificacion() != null && !form.getNumeroIdentificacion().isBlank()
                && identificacionRepository.existsByNumero(form.getNumeroIdentificacion().trim())) {
            br.rejectValue("numeroIdentificacion", "identificacion.exists", "El número de identificación ya está registrado");
        }

        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            br.rejectValue("confirmPassword", "password.no.match", "Las contraseñas no coinciden");
        }

        validarDominioCorreo(form.getCorreo(), br);
        validarPoliticaContrasena(form.getPassword(), br);
    }

    private void validarRegistroVisualizador(RegistroVisualizadorForm form, BindingResult br) {
        if (form.getCorreo() != null && !form.getCorreo().isBlank()
                && usuarioRepository.existsByCorreoIgnoreCase(form.getCorreo().trim())) {
            br.rejectValue("correo", "correo.exists", "El correo ya está registrado");
        }

        if (form.getNumeroIdentificacion() != null && !form.getNumeroIdentificacion().isBlank()
                && identificacionRepository.existsByNumero(form.getNumeroIdentificacion().trim())) {
            br.rejectValue("numeroIdentificacion", "identificacion.exists", "El DNI ya está registrado");
        }

        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            br.rejectValue("confirmPassword", "password.no.match", "Las contraseñas no coinciden");
        }

        validarDominioCorreo(form.getCorreo(), br);
        validarPoliticaContrasena(form.getPassword(), br);
    }

    private void validarDominioCorreo(String correo, BindingResult br) {
        if (correo == null || correo.isBlank()) {
            return;
        }

        String correoNormalizado = correo.trim().toLowerCase();
        int posicionArroba = correoNormalizado.lastIndexOf("@");

        if (posicionArroba == -1) {
            return;
        }

        String dominioCorreo = correoNormalizado.substring(posicionArroba);

        boolean autorizado = dominioAutorizadoRepository.findByEstadoTrue()
                .stream()
                .map(DominioAutorizado::getNombreDominio)
                .filter(d -> d != null && !d.isBlank())
                .map(d -> d.trim().toLowerCase())
                .anyMatch(dominioCorreo::equals);

        if (!autorizado) {
            br.rejectValue(
                    "correo",
                    "correo.dominio.no.autorizado",
                    "El dominio del correo no está autorizado"
            );
        }
    }

    private void validarPoliticaContrasena(String password, BindingResult br) {
        if (password == null || password.isBlank()) {
            return;
        }

        PoliticaContrasena politica = politicaContrasenaRepository.findById(1)
                .orElse(null);

        if (politica == null) {
            return;
        }

        if (politica.getLongitudMinima() != null
                && password.length() < politica.getLongitudMinima()) {
            br.rejectValue(
                    "password",
                    "password.longitud",
                    "La contraseña debe tener mínimo " + politica.getLongitudMinima() + " caracteres"
            );
        }

        if (Boolean.TRUE.equals(politica.getRequiereMayuscula())
                && !password.matches(".*[A-ZÁÉÍÓÚÑ].*")) {
            br.rejectValue(
                    "password",
                    "password.mayuscula",
                    "La contraseña debe tener al menos una mayúscula"
            );
        }

        if (Boolean.TRUE.equals(politica.getRequiereNumero())
                && !password.matches(".*\\d.*")) {
            br.rejectValue(
                    "password",
                    "password.numero",
                    "La contraseña debe tener al menos un número"
            );
        }

        if (Boolean.TRUE.equals(politica.getRequiereSimbolo())
                && !password.matches(".*[^A-Za-zÁÉÍÓÚáéíóúÑñ0-9].*")) {
            br.rejectValue(
                    "password",
                    "password.simbolo",
                    "La contraseña debe tener al menos un símbolo"
            );
        }
    }

    private void cargarPoliticaPasswordEnModelo(Model model) {
        PoliticaContrasena politica = politicaContrasenaRepository.findById(1)
                .orElse(null);

        model.addAttribute(
                "passwordMinLength",
                politica != null && politica.getLongitudMinima() != null
                        ? politica.getLongitudMinima()
                        : 10
        );

        model.addAttribute(
                "passwordRequireUpper",
                politica == null || Boolean.TRUE.equals(politica.getRequiereMayuscula())
        );

        model.addAttribute(
                "passwordRequireNumber",
                politica == null || Boolean.TRUE.equals(politica.getRequiereNumero())
        );

        model.addAttribute(
                "passwordRequireSymbol",
                politica == null || Boolean.TRUE.equals(politica.getRequiereSimbolo())
        );
    }
}
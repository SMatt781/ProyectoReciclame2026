package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.RegistroSocioForm;
import com.example.proyectoreciclame.Dto.RegistroVisualizadorForm;
import com.example.proyectoreciclame.Entity.DominioAutorizado;
import com.example.proyectoreciclame.Entity.PoliticaContrasena;
import com.example.proyectoreciclame.Entity.Rol;
import com.example.proyectoreciclame.Entity.SolicitudRegistro;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Entity.UsuarioEmpresa;
import com.example.proyectoreciclame.Repository.DominioAutorizadoRepository;
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

    public AuthController(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          UsuarioEmpresaRepository usuarioEmpresaRepository,
                          SolicitudRegistroRepository solicitudRegistroRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          CorreoService correoService,
                          DominioAutorizadoRepository dominioAutorizadoRepository,
                          PoliticaContrasenaRepository politicaContrasenaRepository,
                          RegistroSesionRepository registroSesionRepository,
                          AdminNotificacionController adminNotificacionController) {
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
        usuario.setNombres(form.getNombres().trim());
        usuario.setApellidoPaterno(form.getApellidoPaterno().trim());
        usuario.setApellidoMaterno(form.getApellidoMaterno().trim());
        usuario.setDni(form.getDni().trim());
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

        UsuarioEmpresa ue = new UsuarioEmpresa();
        ue.setUsuario(usuario);
        ue.setRuc(form.getRuc().trim());
        ue.setRazonSocial("Pendiente de completar");
        ue.setCargo(null);
        usuarioEmpresaRepository.save(ue);

        SolicitudRegistro solicitud = new SolicitudRegistro();
        solicitud.setNombres(usuario.getNombres());
        solicitud.setApellidoPaterno(usuario.getApellidoPaterno());
        solicitud.setApellidoMaterno(usuario.getApellidoMaterno());
        solicitud.setDni(usuario.getDni());
        solicitud.setCorreo(usuario.getCorreo());
        solicitud.setTelefono(usuario.getTelefono());
        solicitud.setRuc(
                form.getRuc() != null
                        ? form.getRuc().trim()
                        : null
        );
        solicitud.setRolSolicitado("SOCIO");
        solicitud.setEstado("PENDIENTE");
        solicitud.setMotivoRechazo(null);
        solicitud.setRevisadoPor(null);
        solicitud.setIdUsuarioCreado(usuario.getIdUsuario());
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setFechaResolucion(null);

        solicitudRegistroRepository.save(solicitud);

        correoService.enviarConfirmacionRegistro(
                usuario.getCorreo(),
                usuario.getNombres(),
                "SOCIO"
        );

        // Crear notificación para los admins
        adminNotificacionController.crearNotificacionAdmin(
                "Nueva solicitud de registro - Socio",
                usuario.getNombres() + " " + usuario.getApellidoPaterno() + " ha solicitado registrarse como SOCIO. Correo: " + usuario.getCorreo(),
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
        usuario.setDni(form.getDni().trim());
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

        SolicitudRegistro solicitud = new SolicitudRegistro();
        solicitud.setNombres(usuario.getNombres());
        solicitud.setApellidoPaterno(usuario.getApellidoPaterno());
        solicitud.setApellidoMaterno(usuario.getApellidoMaterno());
        solicitud.setDni(usuario.getDni());
        solicitud.setCorreo(usuario.getCorreo());
        solicitud.setTelefono(usuario.getTelefono());
        solicitud.setRuc(null);
        solicitud.setRolSolicitado("VISUALIZADOR");
        solicitud.setEstado("PENDIENTE");
        solicitud.setMotivoRechazo(null);
        solicitud.setRevisadoPor(null);
        solicitud.setIdUsuarioCreado(usuario.getIdUsuario());
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setFechaResolucion(null);

        solicitudRegistroRepository.save(solicitud);

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
        if (form.getCorreo() != null && !form.getCorreo().isBlank()
                && usuarioRepository.existsByCorreoIgnoreCase(form.getCorreo().trim())) {
            br.rejectValue("correo", "correo.exists", "El correo ya está registrado");
        }

        if (form.getDni() != null && !form.getDni().isBlank()
                && usuarioRepository.existsByDni(form.getDni().trim())) {
            br.rejectValue("dni", "dni.exists", "El DNI ya está registrado");
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

        if (form.getDni() != null && !form.getDni().isBlank()
                && usuarioRepository.existsByDni(form.getDni().trim())) {
            br.rejectValue("dni", "dni.exists", "El DNI ya está registrado");
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
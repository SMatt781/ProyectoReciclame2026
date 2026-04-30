package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.RegistroSocioForm;
import com.example.proyectoreciclame.Dto.RegistroVisualizadorForm;
import com.example.proyectoreciclame.Entity.Rol;
import com.example.proyectoreciclame.Entity.SolicitudRegistro;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Entity.UsuarioEmpresa;
import com.example.proyectoreciclame.Repository.RolRepository;
import com.example.proyectoreciclame.Repository.SolicitudRegistroRepository;
import com.example.proyectoreciclame.Repository.UsuarioEmpresaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.CorreoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.Arrays;

@Controller
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final SolicitudRegistroRepository solicitudRegistroRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CorreoService correoService;

    public AuthController(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          UsuarioEmpresaRepository usuarioEmpresaRepository,
                          SolicitudRegistroRepository solicitudRegistroRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          CorreoService correoService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.solicitudRegistroRepository = solicitudRegistroRepository;
        this.passwordEncoder = passwordEncoder;
        this.correoService = correoService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/post-login")
    public String postLogin(Authentication authentication) {
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
        return "auth/registro-socio";
    }

    @PostMapping("/registro/socio")
    public String registrarSocio(@Valid @ModelAttribute("registroForm") RegistroSocioForm form,
                                 BindingResult bindingResult) {

        validarRegistroSocio(form, bindingResult);

        if (bindingResult.hasErrors()) {
            return "auth/registro-socio";
        }

        Rol rolSocio = rolRepository.findByNombreIgnoreCase("SOCIO")
                .orElseThrow(() -> new RuntimeException("Rol SOCIO no existe"));

        Usuario usuario = new Usuario();
        usuario.setRol(rolSocio);
        usuario.setNombres(form.getNombres().trim());
        usuario.setApellidoPaterno(extraerApellidoPaterno(form.getApellidos()));
        usuario.setApellidoMaterno(extraerApellidoMaterno(form.getApellidos()));
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
        solicitud.setRuc(form.getRuc().trim());
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

        return "redirect:/solicitud-enviada";
    }

    @GetMapping("/registro/visualizador")
    public String registroVisualizador(Model model) {
        model.addAttribute("registroForm", new RegistroVisualizadorForm());
        return "auth/registro-visualizador";
    }

    @PostMapping("/registro/visualizador")
    public String registrarVisualizador(@Valid @ModelAttribute("registroForm") RegistroVisualizadorForm form,
                                        BindingResult bindingResult) {

        validarRegistroVisualizador(form, bindingResult);

        if (bindingResult.hasErrors()) {
            return "auth/registro-visualizador";
        }

        Rol rolVisualizador = rolRepository.findByNombreIgnoreCase("VISUALIZADOR")
                .orElseThrow(() -> new RuntimeException("Rol VISUALIZADOR no existe"));

        Usuario usuario = new Usuario();
        usuario.setRol(rolVisualizador);
        usuario.setNombres(form.getNombres().trim());
        usuario.setApellidoPaterno(extraerApellidoPaterno(form.getApellidos()));
        usuario.setApellidoMaterno(extraerApellidoMaterno(form.getApellidos()));
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
        if (usuarioRepository.existsByCorreoIgnoreCase(form.getCorreo().trim())) {
            br.rejectValue("correo", "correo.exists", "El correo ya está registrado");
        }

        if (usuarioRepository.existsByDni(form.getDni().trim())) {
            br.rejectValue("dni", "dni.exists", "El DNI ya está registrado");
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            br.rejectValue("confirmPassword", "password.no.match", "Las contraseñas no coinciden");
        }
    }

    private void validarRegistroVisualizador(RegistroVisualizadorForm form, BindingResult br) {
        if (usuarioRepository.existsByCorreoIgnoreCase(form.getCorreo().trim())) {
            br.rejectValue("correo", "correo.exists", "El correo ya está registrado");
        }

        if (usuarioRepository.existsByDni(form.getDni().trim())) {
            br.rejectValue("dni", "dni.exists", "El DNI ya está registrado");
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            br.rejectValue("confirmPassword", "password.no.match", "Las contraseñas no coinciden");
        }
    }

    private String extraerApellidoPaterno(String apellidos) {
        if (apellidos == null || apellidos.isBlank()) {
            return "";
        }
        String[] partes = apellidos.trim().split("\\s+");
        return partes.length >= 1 ? partes[0] : "";
    }

    private String extraerApellidoMaterno(String apellidos) {
        if (apellidos == null || apellidos.isBlank()) {
            return "";
        }
        String[] partes = apellidos.trim().split("\\s+");
        return partes.length >= 2
                ? String.join(" ", Arrays.copyOfRange(partes, 1, partes.length))
                : "";
    }
}
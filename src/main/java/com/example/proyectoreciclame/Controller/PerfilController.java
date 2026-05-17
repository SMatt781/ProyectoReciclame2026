package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Entity.PoliticaContrasena;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Entity.UsuarioEmpresa;
import com.example.proyectoreciclame.Repository.PoliticaContrasenaRepository;
import com.example.proyectoreciclame.Repository.UsuarioEmpresaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class PerfilController {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PoliticaContrasenaRepository politicaContrasenaRepository;

    public PerfilController(UsuarioRepository usuarioRepository,
                            UsuarioEmpresaRepository usuarioEmpresaRepository,
                            BCryptPasswordEncoder passwordEncoder,
                            PoliticaContrasenaRepository politicaContrasenaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.politicaContrasenaRepository = politicaContrasenaRepository;
    }

    @GetMapping("/perfil")
    public String verPerfil(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        String correo = principal.getName();
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + correo));

        model.addAttribute("usuario", usuario);

        usuarioEmpresaRepository.findByUsuario(usuario)
                .ifPresent(empresa -> model.addAttribute("usuarioEmpresa", empresa));

        // Cargar política de contraseña
        cargarPoliticaEnModelo(model);

        return "perfilUsuario";
    }

    @PostMapping("/perfil/guardar")
    public String guardarPerfil(
            @RequestParam(required = false) String nombres,
            @RequestParam(required = false) String apellidoPaterno,
            @RequestParam(required = false) String apellidoMaterno,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String razonSocial,
            @RequestParam(required = false) String ruc,
            @RequestParam(required = false) String cargo,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) return "redirect:/login";

        String correo = principal.getName();
        Usuario usuarioBD = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (nombres != null) usuarioBD.setNombres(nombres);
        if (apellidoPaterno != null) usuarioBD.setApellidoPaterno(apellidoPaterno);
        if (apellidoMaterno != null) usuarioBD.setApellidoMaterno(apellidoMaterno);
        if (telefono != null) usuarioBD.setTelefono(telefono);

        usuarioRepository.save(usuarioBD);

        if (razonSocial != null || ruc != null || cargo != null) {
            UsuarioEmpresa empresa = usuarioEmpresaRepository.findByUsuario(usuarioBD)
                    .orElse(new UsuarioEmpresa());
            empresa.setUsuario(usuarioBD);
            if (ruc != null) empresa.setRuc(ruc);
            if (razonSocial != null) empresa.setRazonSocial(razonSocial);
            if (cargo != null) empresa.setCargo(cargo);
            usuarioEmpresaRepository.save(empresa);
        }

        redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente.");
        return "redirect:/perfil";
    }

    @PostMapping("/perfil/cambiar-contrasena")
    public String cambiarContrasena(
            @RequestParam String contrasenaActual,
            @RequestParam String nuevaContrasena,
            @RequestParam String confirmarContrasena,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) return "redirect:/login";

        // Validar que coincidan
        if (!nuevaContrasena.equals(confirmarContrasena)) {
            redirectAttributes.addFlashAttribute("errorContrasena", "Las contraseñas nuevas no coinciden.");
            return "redirect:/perfil";
        }

        String correo = principal.getName();
        Usuario usuarioBD = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar contraseña actual
        if (!passwordEncoder.matches(contrasenaActual, usuarioBD.getContrasenaHash())) {
            redirectAttributes.addFlashAttribute("errorContrasena", "La contraseña actual es incorrecta.");
            return "redirect:/perfil";
        }

        // Validar que la nueva no sea igual a la actual
        if (passwordEncoder.matches(nuevaContrasena, usuarioBD.getContrasenaHash())) {
            redirectAttributes.addFlashAttribute("errorContrasena", "La nueva contraseña no puede ser igual a la actual.");
            return "redirect:/perfil";
        }

        // Validar política de contraseña
        String errorPolitica = validarPolitica(nuevaContrasena);
        if (errorPolitica != null) {
            redirectAttributes.addFlashAttribute("errorContrasena", errorPolitica);
            return "redirect:/perfil";
        }

        usuarioBD.setContrasenaHash(passwordEncoder.encode(nuevaContrasena));
        usuarioBD.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(usuarioBD);

        redirectAttributes.addFlashAttribute("mensajeContrasena", "Contraseña actualizada correctamente.");
        return "redirect:/perfil";
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private void cargarPoliticaEnModelo(Model model) {
        PoliticaContrasena p = politicaContrasenaRepository.findById(1).orElse(null);
        model.addAttribute("pwdMinLen",      p != null && p.getLongitudMinima() != null ? p.getLongitudMinima() : 8);
        model.addAttribute("pwdMayuscula",   p == null || Boolean.TRUE.equals(p.getRequiereMayuscula()));
        model.addAttribute("pwdNumero",      p == null || Boolean.TRUE.equals(p.getRequiereNumero()));
        model.addAttribute("pwdSimbolo",     p == null || Boolean.TRUE.equals(p.getRequiereSimbolo()));
    }

    private String validarPolitica(String password) {
        PoliticaContrasena p = politicaContrasenaRepository.findById(1).orElse(null);
        if (p == null) return null;

        if (p.getLongitudMinima() != null && password.length() < p.getLongitudMinima())
            return "La contraseña debe tener mínimo " + p.getLongitudMinima() + " caracteres.";
        if (Boolean.TRUE.equals(p.getRequiereMayuscula()) && !password.matches(".*[A-ZÁÉÍÓÚÑ].*"))
            return "La contraseña debe tener al menos una mayúscula.";
        if (Boolean.TRUE.equals(p.getRequiereNumero()) && !password.matches(".*\\d.*"))
            return "La contraseña debe tener al menos un número.";
        if (Boolean.TRUE.equals(p.getRequiereSimbolo()) && !password.matches(".*[^A-Za-zÁÉÍÓÚáéíóúÑñ0-9].*"))
            return "La contraseña debe tener al menos un símbolo.";

        return null;
    }
}
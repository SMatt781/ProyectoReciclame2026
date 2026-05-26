package com.example.proyectoreciclame.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;
import com.example.proyectoreciclame.Entity.PoliticaContrasena;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Entity.UsuarioEmpresa;
import com.example.proyectoreciclame.Repository.PoliticaContrasenaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Repository.UsuarioEmpresaRepository;
import com.example.proyectoreciclame.Service.PerfilFotoService;
import com.example.proyectoreciclame.Dto.ActualizarFotoForm;
import java.io.IOException;

/**
 * Controlador para gestionar el perfil del usuario
 */
@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioRepository usuarioRepository;
    private final PerfilFotoService perfilFotoService;
    private final PoliticaContrasenaRepository politicaContrasenaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;

    public PerfilController(UsuarioRepository usuarioRepository,
                            PerfilFotoService perfilFotoService,
                            PoliticaContrasenaRepository politicaContrasenaRepository,
                            PasswordEncoder passwordEncoder,
                            UsuarioEmpresaRepository usuarioEmpresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilFotoService = perfilFotoService;
        this.politicaContrasenaRepository = politicaContrasenaRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
    }

    /** Muestra la página de perfil del usuario autenticado */
    @GetMapping
    public String mostrarPerfil(Authentication authentication, Model model) {
        String correo = authentication.getName();
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UsuarioEmpresa usuarioEmpresa = usuarioEmpresaRepository.findByUsuario(usuario).orElse(null);

        model.addAttribute("usuario", usuario);
        model.addAttribute("usuarioEmpresa", usuarioEmpresa);
        cargarPoliticaEnModelo(model);
        return "perfilUsuario";
    }

    /** Guarda cambios de datos personales */
    @PostMapping("/guardar")
    public String guardarPerfil(Authentication authentication,
                                @RequestParam("nombres") String nombres,
                                @RequestParam("apellidoPaterno") String apellidoPaterno,
                                @RequestParam("apellidoMaterno") String apellidoMaterno,
                                @RequestParam(value = "telefono", required = false) String telefono,
                                @RequestParam(value = "razonSocial", required = false) String razonSocial,
                                RedirectAttributes redirectAttributes) {
        try {
            String correo = authentication.getName();
            Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            usuario.setNombres(nombres != null ? nombres.trim() : "");
            usuario.setApellidoPaterno(apellidoPaterno != null ? apellidoPaterno.trim() : "");
            usuario.setApellidoMaterno(apellidoMaterno != null ? apellidoMaterno.trim() : "");
            usuario.setTelefono(telefono != null ? telefono.trim() : null);
            usuarioRepository.save(usuario);

            if (razonSocial != null && !razonSocial.isBlank()) {
                usuarioEmpresaRepository.findByUsuario(usuario).ifPresent(ue -> {
                    ue.setRazonSocial(razonSocial.trim());
                    usuarioEmpresaRepository.save(ue);
                });
            }

            redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/perfil";
    }

    /** Cambia la contraseña del usuario autenticado */
    @PostMapping("/cambiar-contrasena")
    public String cambiarContrasena(Authentication authentication,
                                    @RequestParam("contrasenaActual") String contrasenaActual,
                                    @RequestParam("nuevaContrasena") String nuevaContrasena,
                                    @RequestParam("confirmarContrasena") String confirmarContrasena,
                                    RedirectAttributes redirectAttributes) {
        try {
            String correo = authentication.getName();
            Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            if (!passwordEncoder.matches(contrasenaActual, usuario.getContrasenaHash())) {
                redirectAttributes.addFlashAttribute("errorContrasena", "La contraseña actual es incorrecta.");
                return "redirect:/perfil";
            }

            if (!nuevaContrasena.equals(confirmarContrasena)) {
                redirectAttributes.addFlashAttribute("errorContrasena", "Las contraseñas no coinciden.");
                return "redirect:/perfil";
            }

            String errorPolitica = validarPolitica(nuevaContrasena);
            if (errorPolitica != null) {
                redirectAttributes.addFlashAttribute("errorContrasena", errorPolitica);
                return "redirect:/perfil";
            }

            usuario.setContrasenaHash(passwordEncoder.encode(nuevaContrasena));
            usuarioRepository.save(usuario);

            redirectAttributes.addFlashAttribute("mensajeContrasena", "Contraseña actualizada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorContrasena", "Error al cambiar contraseña: " + e.getMessage());
        }
        return "redirect:/perfil";
    }

    /** Actualiza la foto de perfil del usuario autenticado */
    @PostMapping("/actualizar-foto")
    public String actualizarFoto(@RequestParam("foto") org.springframework.web.multipart.MultipartFile foto,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            String correo = authentication.getName();
            Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            perfilFotoService.guardarFoto(usuario.getIdUsuario(), foto);

            redirectAttributes.addFlashAttribute("mensaje", "¡Foto de perfil actualizada correctamente!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar la imagen: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/perfil";
    }

    /** Elimina la foto de perfil del usuario autenticado */
    @PostMapping("/eliminar-foto")
    public String eliminarFoto(Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String correo = authentication.getName();
            Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            perfilFotoService.eliminarFoto(usuario.getIdUsuario());

            redirectAttributes.addFlashAttribute("mensaje", "Foto de perfil eliminada correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/perfil";
    }

    private void cargarPoliticaEnModelo(Model model) {
        PoliticaContrasena p = politicaContrasenaRepository.findById(1).orElse(null);
        model.addAttribute("pwdMinLen",    p != null && p.getLongitudMinima() != null ? p.getLongitudMinima() : 8);
        model.addAttribute("pwdMayuscula", p == null || Boolean.TRUE.equals(p.getRequiereMayuscula()));
        model.addAttribute("pwdNumero",    p == null || Boolean.TRUE.equals(p.getRequiereNumero()));
        model.addAttribute("pwdSimbolo",   p == null || Boolean.TRUE.equals(p.getRequiereSimbolo()));
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

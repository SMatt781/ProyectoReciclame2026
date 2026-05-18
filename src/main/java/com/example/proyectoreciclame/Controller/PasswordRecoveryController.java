package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.NuevaPasswordForm;
import com.example.proyectoreciclame.Dto.RecuperarPasswordForm;
import com.example.proyectoreciclame.Dto.VerificarCodigoRecuperacionForm;
import com.example.proyectoreciclame.Entity.PoliticaContrasena;
import com.example.proyectoreciclame.Repository.PoliticaContrasenaRepository;
import com.example.proyectoreciclame.Service.RecuperacionPasswordService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/password")
public class PasswordRecoveryController {

    private final RecuperacionPasswordService recuperacionPasswordService;
    private final PoliticaContrasenaRepository politicaContrasenaRepository;

    public PasswordRecoveryController(RecuperacionPasswordService recuperacionPasswordService,
                                      PoliticaContrasenaRepository politicaContrasenaRepository) {
        this.recuperacionPasswordService = recuperacionPasswordService;
        this.politicaContrasenaRepository = politicaContrasenaRepository;
    }

    @GetMapping("/recuperar")
    public String verRecuperar(Model model) {
        model.addAttribute("form", new RecuperarPasswordForm());
        return "auth/RecuperarContraseña";
    }

    @PostMapping("/recuperar")
    public String procesarRecuperar(@Valid @ModelAttribute("form") RecuperarPasswordForm form,
                                    BindingResult br,
                                    Model model) {
        if (br.hasErrors()) {
            return "auth/RecuperarContraseña";
        }

        String correo = form.getCorreo().trim().toLowerCase();

        if (!recuperacionPasswordService.usuarioPuedeRecuperar(correo)) {
            model.addAttribute("error", "El correo no existe o la cuenta no está habilitada.");
            return "auth/RecuperarContraseña";
        }

        recuperacionPasswordService.generarYEnviarCodigo(correo);

        VerificarCodigoRecuperacionForm codigoForm = new VerificarCodigoRecuperacionForm();
        codigoForm.setCorreo(correo);
        model.addAttribute("form", codigoForm);

        return "auth/VerificaciónCódigoRecuperación";
    }

    @PostMapping("/verificar")
    public String verificarCodigo(@Valid @ModelAttribute("form") VerificarCodigoRecuperacionForm form,
                                  BindingResult br,
                                  Model model) {
        if (br.hasErrors()) {
            return "auth/VerificaciónCódigoRecuperación";
        }

        boolean valido = recuperacionPasswordService.validarCodigo(
                form.getCorreo().trim().toLowerCase(),
                form.getCodigo().trim()
        );

        if (!valido) {
            model.addAttribute("error", "El código es inválido o ha expirado.");
            return "auth/VerificaciónCódigoRecuperación";
        }

        NuevaPasswordForm nuevaForm = new NuevaPasswordForm();
        nuevaForm.setCorreo(form.getCorreo().trim().toLowerCase());
        nuevaForm.setCodigo(form.getCodigo().trim());

        model.addAttribute("form", nuevaForm);
        cargarPoliticaEnModelo(model);
        return "auth/NuevaContraseña";
    }

    @PostMapping("/nueva")
    public String guardarNuevaPassword(@Valid @ModelAttribute("form") NuevaPasswordForm form,
                                       BindingResult br,
                                       Model model) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            br.rejectValue("confirmPassword", "error.confirmPassword", "Las contraseñas no coinciden");
        }

        if (br.hasErrors()) {
            cargarPoliticaEnModelo(model);
            return "auth/NuevaContraseña";
        }

        String errorPolitica = validarPolitica(form.getPassword());
        if (errorPolitica != null) {
            model.addAttribute("error", errorPolitica);
            cargarPoliticaEnModelo(model);
            return "auth/NuevaContraseña";
        }

        boolean ok = recuperacionPasswordService.cambiarPassword(
                form.getCorreo().trim().toLowerCase(),
                form.getCodigo().trim(),
                form.getPassword()
        );

        if (!ok) {
            model.addAttribute("error", "No se pudo actualizar la contraseña. El código puede haber expirado.");
            cargarPoliticaEnModelo(model);
            return "auth/NuevaContraseña";
        }

        return "auth/NuevaContraseñaExitosa";
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
package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.NuevaPasswordForm;
import com.example.proyectoreciclame.Dto.RecuperarPasswordForm;
import com.example.proyectoreciclame.Dto.VerificarCodigoRecuperacionForm;
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

    public PasswordRecoveryController(RecuperacionPasswordService recuperacionPasswordService) {
        this.recuperacionPasswordService = recuperacionPasswordService;
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
            return "auth/NuevaContraseña";
        }

        boolean ok = recuperacionPasswordService.cambiarPassword(
                form.getCorreo().trim().toLowerCase(),
                form.getCodigo().trim(),
                form.getPassword()
        );

        if (!ok) {
            model.addAttribute("error", "No se pudo actualizar la contraseña. El código puede haber expirado.");
            return "auth/NuevaContraseña";
        }

        return "auth/NuevaContraseñaExitosa";
    }
}
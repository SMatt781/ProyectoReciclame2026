package com.example.proyectoreciclame.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class LegalController {

    private static final Logger logger = LoggerFactory.getLogger(LegalController.class);
    private static final String SOPORTE_EMAIL = "reciclame.soporte@gmail.com";

    @GetMapping("/terminos")
    public String verTerminos(Model model) {
        model.addAttribute("pageTitle", "Términos y Condiciones");
        return "legal/terminos";
    }

    @GetMapping("/privacidad")
    public String verPrivacidad(Model model) {
        model.addAttribute("pageTitle", "Política de Privacidad");
        return "legal/privacidad";
    }

    @GetMapping("/soporte")
    public String verSoporte(Model model) {
        model.addAttribute("pageTitle", "Contacto - Soporte");
        model.addAttribute("soporteEmail", SOPORTE_EMAIL);
        return "legal/soporte";
    }

    /**
     * Procesa el formulario de contacto de soporte.
     * En producción, esto debería enviar un email o guardar en BD.
     * Por ahora, lo registramos en logs.
     */
    @PostMapping("/soporte/enviar")
    public String enviarMensajeSoporte(
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam String asunto,
            @RequestParam String mensaje,
            @RequestParam(required = false) boolean aceptaPrivacidad,
            RedirectAttributes redirectAttributes) {

        // Validaciones
        if (nombre == null || nombre.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El nombre es requerido");
            return "redirect:/soporte";
        }

        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            redirectAttributes.addFlashAttribute("error", "Correo electrónico inválido");
            return "redirect:/soporte";
        }

        if (asunto == null || asunto.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar un asunto");
            return "redirect:/soporte";
        }

        if (mensaje == null || mensaje.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El mensaje no puede estar vacío");
            return "redirect:/soporte";
        }

        if (!aceptaPrivacidad) {
            redirectAttributes.addFlashAttribute("error", "Debe aceptar la Política de Privacidad");
            return "redirect:/soporte";
        }

        // ── Registrar mensaje en logs ─────────────────────────────────────
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.info("NUEVO MENSAJE DE SOPORTE RECIBIDO");
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.info("Nombre: {}", nombre);
        logger.info("Email: {}", email);
        logger.info("Asunto: {}", asunto);
        logger.info("Mensaje: {}", mensaje);
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // ── TODO: En producción, implementar: ─────────────────────────────
        // 1. Guardar en tabla de soporte_mensajes
        // 2. Enviar email a admin
        // 3. Enviar email de confirmación a usuario
        // ──────────────────────────────────────────────────────────────────

        redirectAttributes.addFlashAttribute("success",
                "Gracias por tu mensaje. Nos pondremos en contacto pronto a " + email);

        return "redirect:/soporte";
    }
}

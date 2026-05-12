package com.example.proyectoreciclame.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccessDeniedController {

    @GetMapping("/acceso-denegado")
    public String accessDenied(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String rolUsuario = "DESCONOCIDO";
        if (authentication != null && authentication.getAuthorities() != null) {
            rolUsuario = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst()
                    .orElse("DESCONOCIDO");
        }

        model.addAttribute("rolUsuario", rolUsuario);
        return "error/acceso-denegado";
    }
}
package com.example.proyectoreciclame.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminEstudiosController {

    @GetMapping("/admin/estudios")
    public String estudiosAdmin() {
        return "admin/estudio_main_admin";
    }
    @PostMapping("/admin/estudio_new")
    public String guardarEstudio() {
        // guardar en BD aquí

        return "redirect:/admin/estudios";
    }

    @GetMapping("/admin/normativas")
    public String normativasAdmin() {
        return "admin/repo_main_admin";
    }
    @PostMapping("/admin/repo_new")
    public String guardarNormativa() {
        // guardar en BD aquí

        return "redirect:/admin/normativas";
    }
}
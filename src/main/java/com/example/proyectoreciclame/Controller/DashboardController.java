package com.example.proyectoreciclame.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("currentSection", "admin-dashboard");
        return "admin/dashboard_admin";
    }

    @GetMapping("/admin/estudio_new")
    public String nuevoEstudio(Model model) {
        model.addAttribute("currentSection", "admin-estudios");
        return "admin/estudio_new";
    }
}
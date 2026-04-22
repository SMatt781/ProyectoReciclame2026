package com.example.proyectoreciclame.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard_admin";
    }

    @GetMapping("/admin/estudio_new")
    public String nuevoEstudio() {
        return "admin/estudio_new";
    }
}
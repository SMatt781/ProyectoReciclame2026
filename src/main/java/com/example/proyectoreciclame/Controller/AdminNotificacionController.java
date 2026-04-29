package com.example.proyectoreciclame.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminNotificacionController {

    @GetMapping("/admin/notificaciones")
    public String verNotificaciones() {
        return "admin/notificaciones";
    }
}
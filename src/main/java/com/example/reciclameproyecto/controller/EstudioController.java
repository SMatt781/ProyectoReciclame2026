package com.example.reciclameproyecto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/estudios")
public class EstudioController {

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        // Datos de ejemplo (REEMPLAZAR con datos reales de BD)
        List<Map<String, Object>> estudios = List.of(
                Map.of(
                        "id", 1L,
                        "titulo", "Análisis de Ciclo de Vida de Envases PET en Perú",
                        "descripcion", "Estudio exhaustivo sobre el impacto ambiental...",
                        "estado", "VIGENTE",
                        "formato", "PDF",
                        "fechaPublicacion", LocalDate.of(2024, 1, 15)),
                Map.of(
                        "id", 2L,
                        "titulo", "Mapeo de Actores del Reciclaje Inclusivo",
                        "descripcion", "Documento de trabajo preliminar que identifica...",
                        "estado", "BORRADOR",
                        "formato", "DOCX",
                        "fechaPublicacion", LocalDate.of(2024, 2, 20)));

        model.addAttribute("estudios", estudios);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 5);
        model.addAttribute("currentPage", "estudios"); // Para sidebar
        model.addAttribute("usuarioNombre", "Paola Flores");
        model.addAttribute("usuarioRol", "SOCIO");
        model.addAttribute("usuarioAvatar", "/images/avatar.jpg");

        return "estudios";
    }

    @GetMapping("/panelSocio")
    public String panelSocio() {
        return "socio/panelPrincipal";
    }

    @GetMapping("/normativas")
    public String normativas() {
        return "socio/repoNormativo";
    }

    @GetMapping("/normativasVisu")
    public String normativasVisu() {
        return "socio/visualizadorNormativa";
    }

    @GetMapping("/estudiosPagina")
    public String estudios() {
        return "socio/estudios";
    }

    @GetMapping("/estudiosPaginaVisualizador")
    public String estudiosPaginaVisualizador() {
        return "socio/visualizadorEstudio";
    }

    @GetMapping("/estudiosPaginaVisualizadorDescargable")
    public String estudiosPaginaVisualizadorDescargable() {
        return "socio/visualizadorEstudioDescargable";
    }

}
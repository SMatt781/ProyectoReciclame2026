package com.example.reciclameproyecto.controller;

import com.example.reciclameproyecto.DTO.NormativaDTO;
import com.example.reciclameproyecto.entity.Normativa;
import com.example.reciclameproyecto.repository.NormativaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/normativas")
public class NormativaController {

    @Autowired
    private NormativaRepository normativaRepository;

    @GetMapping
    public String listarNormativas(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "yearSelect") Integer anio,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd,
            @RequestParam(required = false) List<String> categoria,
            @RequestParam(required = false) List<String> estado,
            @RequestParam(required = false) List<String> acceso,
            @RequestParam(required = false) List<String> obligatoriedad,
            Model model) {

        List<Normativa> normativas;

        if (search != null && !search.trim().isEmpty()) {
            normativas = normativaRepository.findByKeyword(search);
        } else if (anio != null || estado != null || acceso != null || categoria != null || obligatoriedad != null || dateStart != null || dateEnd != null) {
            boolean hasAnio = anio != null;
            boolean hasEstados = estado != null && !estado.isEmpty();
            boolean hasAccesos = acceso != null && !acceso.isEmpty();
            boolean hasObligatoriedad = obligatoriedad != null && !obligatoriedad.isEmpty();
            boolean hasCategoria = categoria != null && !categoria.isEmpty();

            java.time.LocalDateTime fechaInicio = null;
            java.time.LocalDateTime fechaFin = null;

            if (dateStart != null && !dateStart.isEmpty()) {
                fechaInicio = java.time.LocalDate.parse(dateStart).atStartOfDay();
            }
            if (dateEnd != null && !dateEnd.isEmpty()) {
                fechaFin = java.time.LocalDate.parse(dateEnd).atTime(23, 59, 59);
            }

            if (hasEstados) {
                estado = estado.stream().map(e -> e.replace(" ", "_")).collect(Collectors.toList());
            }

            normativas = normativaRepository.findWithAdvancedFilters(hasAnio, anio, fechaInicio, fechaFin, hasEstados, estado, hasAccesos,
                    acceso, hasObligatoriedad, obligatoriedad, hasCategoria, categoria);
        } else {
            normativas = normativaRepository.findAllNormativas();
        }

        List<NormativaDTO> normativasDTO = normativas.stream()
                .map(NormativaDTO::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("normativas", normativasDTO);
        model.addAttribute("currentPage", "repoNormativo"); // Para sidebar
        model.addAttribute("usuarioNombre", "Paola Flores");
        model.addAttribute("usuarioRol", "SOCIO");
        model.addAttribute("usuarioAvatar", "/images/avatar.jpg");

        return "socio/repoNormativo";
    }

    @GetMapping("/{id}")
    public String verNormativa(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        Normativa normativa = normativaRepository.findById(id).orElse(null);
        if (normativa == null) {
            return "redirect:/normativas";
        }
        
        model.addAttribute("normativa", normativa);
        model.addAttribute("currentPage", "repoNormativo");
        model.addAttribute("usuarioNombre", "Paola Flores");
        model.addAttribute("usuarioRol", "SOCIO");
        model.addAttribute("usuarioAvatar", "/images/avatar.jpg");

        if (normativa.getAcceso() == Normativa.AccesoNormativa.PAGO) {
            return "socio/visualizadorNormativaPago";
        } else {
            return "socio/visualizadorNormativa";
        }
    }
}

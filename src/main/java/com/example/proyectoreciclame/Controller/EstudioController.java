package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.EstudioDTO;
import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.RegistroDescargaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/estudios")
public class EstudioController {

    @Autowired
    private EstudioRepository estudioRepository;

    @Autowired
    private RegistroDescargaRepository registroDescargaRepository;

    @GetMapping
    public String listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "yearSelect") Integer anio,
            @RequestParam(required = false, name = "dateStart") String dateStartStr,
            @RequestParam(required = false, name = "dateEnd") String dateEndStr,
            @RequestParam(required = false) List<String> format,
            @RequestParam(required = false) List<String> estado,
            Model model) {

        List<EstudioDTO> estudios;

        if (search != null && !search.trim().isEmpty()) {
            estudios = estudioRepository.findByTituloContainingIgnoreCaseDTO(search);
        } else if (anio != null || dateStartStr != null || dateEndStr != null || format != null || estado != null) {
            LocalDate dateStart = (dateStartStr != null && !dateStartStr.isEmpty()) ? LocalDate.parse(dateStartStr)
                    : null;
            LocalDate dateEnd = (dateEndStr != null && !dateEndStr.isEmpty()) ? LocalDate.parse(dateEndStr) : null;

            boolean hasFormatos = format != null && !format.isEmpty();
            boolean hasEstados = estado != null && !estado.isEmpty();

            estudios = estudioRepository.findWithAdvancedFiltersDTO(
                    anio,
                    dateStart,
                    dateEnd,
                    hasFormatos, format,
                    hasEstados, estado);
        } else {
            estudios = estudioRepository.findAllEstudioDTO();
        }

        model.addAttribute("estudios", estudios);
        model.addAttribute("currentPage", "estudios");

        long totalEstudios = estudioRepository.countByEliminadoEnIsNull();
        long totalDescargasEstudios = registroDescargaRepository.countByTipoDocumento("ESTUDIO");
        LocalDate limiteRecientes = LocalDate.now().minusDays(30);
        long estudiosRecientes = estudioRepository.countByFechaPublicacionAfterAndEliminadoEnIsNull(limiteRecientes);
        String anioMasActivo = resolveAnioMasActivo();

        model.addAttribute("totalEstudios", totalEstudios);
        model.addAttribute("totalDescargasEstudios", totalDescargasEstudios);
        model.addAttribute("estudiosRecientes", estudiosRecientes);
        model.addAttribute("anioMasActivo", anioMasActivo);

        return "socio/estudios";
    }

    private String resolveAnioMasActivo() {
        List<Object[]> resultados = estudioRepository.findActiveYearsByPublicacion();
        if (resultados == null || resultados.isEmpty() || resultados.get(0)[0] == null) {
            return "Sin datos";
        }
        return String.valueOf(resultados.get(0)[0]);
    }

//        @GetMapping("/panelSocio")
//    public String panelSocio() {
//        return "socio/panelPrincipal";
//    }
//
//    @GetMapping("/normativas")
//    public String normativas() {
//        return "socio/repoNormativo";
//    }
//
//    @GetMapping("/normativasVisu")
//    public String normativasVisu() {
//        return "socio/visualizadorNormativa";
//    }

    @GetMapping("/{id}")
    public String verEstudio(@PathVariable Long id, Model model) {
        Estudio estudio = estudioRepository.findById(id).orElse(null);
        if (estudio == null) {
            return "redirect:/estudios";
        }
        
        model.addAttribute("estudio", estudio);
        model.addAttribute("currentPage", "estudios");

        if (estudio.getTipoAcceso() == Estudio.TipoAcceso.DESCARGA) {
            return "socio/visualizadorEstudioDescargable";
        } else {
            return "socio/visualizadorEstudio";
        }
    }

}
package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.EstudioDTO;
import com.example.proyectoreciclame.Dto.NormativaDTO;
import com.example.proyectoreciclame.Dto.NormativaDetalleDTO;
import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.Service.NormativaService;
import com.example.proyectoreciclame.util.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/visualizador")
public class VisualizadorController {

    @Autowired
    private EstudioRepository estudioRepository;

    @Autowired
    private NormativaRepository normativaRepository;

    @Autowired
    private NormativaService normativaService;

    @GetMapping
    public String inicio(Model model) {
        model.addAttribute("currentPage", "visualizador");
        return "visualizador/index";
    }

    @GetMapping("/estudios")
    public String listarEstudios(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "yearSelect") Integer anio,
            @RequestParam(required = false, name = "dateStart") String dateStartStr,
            @RequestParam(required = false, name = "dateEnd") String dateEndStr,
            @RequestParam(required = false) List<String> format,
            @RequestParam(required = false) List<String> estado,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        List<EstudioDTO> estudiosFiltrados;

        if (search != null && !search.trim().isEmpty()) {
            estudiosFiltrados = estudioRepository.findByTituloContainingIgnoreCaseDTO(search.trim());

        } else if (anio != null
                || (dateStartStr != null && !dateStartStr.isBlank())
                || (dateEndStr != null && !dateEndStr.isBlank())
                || (format != null && !format.isEmpty())
                || (estado != null && !estado.isEmpty())) {

            LocalDate dateStart = (dateStartStr != null && !dateStartStr.isBlank())
                    ? LocalDate.parse(dateStartStr)
                    : null;

            LocalDate dateEnd = (dateEndStr != null && !dateEndStr.isBlank())
                    ? LocalDate.parse(dateEndStr)
                    : null;

            boolean hasFormatos = format != null && !format.isEmpty();
            boolean hasEstados = estado != null && !estado.isEmpty();

            estudiosFiltrados = estudioRepository.findWithAdvancedFiltersDTO(
                    anio,
                    dateStart,
                    dateEnd,
                    hasFormatos,
                    hasFormatos ? format : List.of(),
                    hasEstados,
                    hasEstados ? estado : List.of()
            );

        } else {
            estudiosFiltrados = estudioRepository.findAllEstudioDTO();
        }

        int pageSize = 6;

        Page<EstudioDTO> paginaEstudios = paginarLista(estudiosFiltrados, page, pageSize);

        model.addAttribute("estudios", paginaEstudios.getContent());
        model.addAttribute("currentPage", "visualizadorEstudios");

        model.addAttribute("page", page);
        model.addAttribute("totalPages", paginaEstudios.getTotalPages());
        model.addAttribute("hasPrevious", paginaEstudios.hasPrevious());
        model.addAttribute("hasNext", paginaEstudios.hasNext());

        model.addAttribute(
                "pageNumbers",
                PaginationUtils.buildPageNumbers(page, paginaEstudios.getTotalPages())
        );

        model.addAttribute("search", search);
        model.addAttribute("anioSeleccionado", anio);
        model.addAttribute("dateStart", dateStartStr);
        model.addAttribute("dateEnd", dateEndStr);
        model.addAttribute("formatosSeleccionados", format);
        model.addAttribute("estadosSeleccionados", estado);

        return "visualizador/estudios";
    }

    @GetMapping("/estudios/{id}")
    public String verEstudio(@PathVariable Long id, Model model) {
        Estudio estudio = estudioRepository.findById(id).orElse(null);

        if (estudio == null) {
            return "redirect:/visualizador/estudios";
        }

        model.addAttribute("estudio", estudio);
        model.addAttribute("currentPage", "visualizadorEstudios");

        return "visualizador/visualizadorEstudio";
    }

    @GetMapping("/normativas")
    public String listarNormativas(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "yearSelect") Integer anio,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd,
            @RequestParam(required = false) List<String> categoria,
            @RequestParam(required = false) List<String> estado,
            @RequestParam(required = false) List<String> alcance,
            @RequestParam(required = false) List<String> obligatoriedad,
            Model model) {

        List<Normativa> normativas;

        boolean hasCategoriaParam = categoria != null && !categoria.isEmpty();
        boolean hasEstadoParam = estado != null && !estado.isEmpty();
        boolean hasAlcanceParam = alcance != null && !alcance.isEmpty();
        boolean hasObligatoriedadParam = obligatoriedad != null && !obligatoriedad.isEmpty();

        if (search != null && !search.trim().isEmpty()) {
            normativas = normativaRepository.findByKeyword(search);
        } else if (anio != null || hasEstadoParam || hasAlcanceParam || hasCategoriaParam || hasObligatoriedadParam || dateStart != null || dateEnd != null) {
            boolean hasAnio = anio != null;
            boolean hasEstados = hasEstadoParam;
            boolean hasAlcance = hasAlcanceParam;
            boolean hasObligatoriedad = hasObligatoriedadParam;
            boolean hasCategoria = hasCategoriaParam;

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

            normativas = normativaRepository.findWithAdvancedFilters(hasAnio, anio, fechaInicio, fechaFin, hasEstados, estado, false,
                    null, hasAlcance, alcance, hasObligatoriedad, obligatoriedad, hasCategoria, categoria);
        } else {
            normativas = normativaRepository.findAllNormativas();
        }

        List<NormativaDTO> normativasDTO = normativas.stream()
                .map(NormativaDTO::fromEntity)
                .collect(Collectors.toList());

        int totalNormas = normativasDTO.size();

        model.addAttribute("normativas", normativasDTO);
        model.addAttribute("currentPage", "visualizadorRepoNormativo");
        model.addAttribute("totalNormas", totalNormas);

        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedYear", anio);
        model.addAttribute("dateStart", dateStart);
        model.addAttribute("dateEnd", dateEnd);
        model.addAttribute("selectedCategorias", categoria);
        model.addAttribute("selectedEstados", estado);
        model.addAttribute("selectedAlcances", alcance);
        model.addAttribute("selectedObligatoriedades", obligatoriedad);

        return "visualizador/normativas";
    }

    @GetMapping("/normativas/{id}")
    public String verNormativa(@PathVariable Long id, Model model) {
        NormativaDetalleDTO detalle = normativaService.obtenerDetalleConContexto(id);
        if (detalle == null || detalle.getNormativa() == null) {
            return "redirect:/visualizador/normativas";
        }

        model.addAttribute("detalle", detalle);
        model.addAttribute("normativa", detalle.getNormativa());
        model.addAttribute("currentPage", "visualizadorRepoNormativo");

        return "visualizador/visualizadorNormativa";
    }

    private Page<EstudioDTO> paginarLista(List<EstudioDTO> lista, int page, int pageSize) {
        if (lista == null || lista.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(0, pageSize), 0);
        }

        int total = lista.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);

        if (page < 0) {
            page = 0;
        }

        if (page >= totalPages) {
            page = totalPages - 1;
        }

        Pageable pageable = PageRequest.of(page, pageSize);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageSize, total);

        List<EstudioDTO> contenido = lista.subList(start, end);

        return new PageImpl<>(contenido, pageable, total);
    }
}

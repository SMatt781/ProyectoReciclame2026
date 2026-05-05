package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.EstudioDTO;
import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.RegistroDescargaRepository;
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
        model.addAttribute("currentPage", "estudios");

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

    private String resolveAnioMasActivo() {
        List<Object[]> resultados = estudioRepository.findActiveYearsByPublicacion();

        if (resultados == null || resultados.isEmpty() || resultados.get(0)[0] == null) {
            return "Sin datos";
        }

        return String.valueOf(resultados.get(0)[0]);
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
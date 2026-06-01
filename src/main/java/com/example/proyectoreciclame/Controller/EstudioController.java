package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.EstudioDTO;
import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.RegistroDescargaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.CitaService;
import com.example.proyectoreciclame.Service.HistorialLecturaService;
import com.example.proyectoreciclame.Service.MiEspacioService;
import com.example.proyectoreciclame.util.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Autowired
    private MiEspacioService miEspacioService;

    @Autowired
    private HistorialLecturaService historialLecturaService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

        // Send all filtered studies to the frontend for client-side pagination
        model.addAttribute("estudios", estudiosFiltrados);
        model.addAttribute("currentPage", "estudios");

        // We can keep these attributes in case other views rely on them, 
        // but client-side JS will handle the actual pagination display
        Page<EstudioDTO> paginaEstudios = paginarLista(estudiosFiltrados, page, 6);
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

        // Fetch related studies
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
        java.util.LinkedHashMap<Long, Estudio> vistos = new java.util.LinkedHashMap<>();
        try {
            List<Estudio> porCat = estudioRepository.findSimilaresPorCategorias(id);
            porCat.forEach(e -> vistos.putIfAbsent(e.getIdEstudio(), e));
            if (vistos.size() < 4) {
                List<Estudio> porAnio = estudioRepository.findSimilaresPorAnio(id, estudio.getAnio());
                porAnio.forEach(e -> vistos.putIfAbsent(e.getIdEstudio(), e));
            }
        } catch (Exception ex) {
            log.warn("[RELACIONADOS] Error: {}", ex.getMessage(), ex);
        }
        List<Estudio> relacionados = vistos.values().stream().limit(4).toList();

        model.addAttribute("estudio", estudio);
        model.addAttribute("estudiosRelacionados", relacionados);
        model.addAttribute("currentPage", "estudios");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean esVisualizador = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VISUALIZADOR"));
        boolean esSocio = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SOCIO"));

        Long idUsuario = resolveIdUsuario(authentication);

        if (esVisualizador) {
            if (idUsuario != null) {
                historialLecturaService.registrarOActualizar(idUsuario, "ESTUDIO", id, estudio.getTitulo());
            }
            model.addAttribute("citas", citaService.generarCitasEstudio(id));
            return "visualizador/visualizadorEstudio";
        } else if (esSocio && idUsuario != null) {
            model.addAttribute("estaGuardado", miEspacioService.estaGuardado(idUsuario, "ESTUDIO", id));
            if (estudio.getTipoAcceso() == Estudio.TipoAcceso.DESCARGA) {
                return "socio/visualizadorEstudioDescargable";
            }
            return "socio/visualizadorEstudio";
        } else if (estudio.getTipoAcceso() == Estudio.TipoAcceso.DESCARGA) {
            return "socio/visualizadorEstudioDescargable";
        } else {
            return "socio/visualizadorEstudio";
        }
    }

    private Long resolveIdUsuario(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) return null;
        return usuarioRepository.findByCorreoWithRol(authentication.getName())
                .map(u -> u.getIdUsuario()).orElse(null);
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
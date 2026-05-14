package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.EstudioDTO;
import com.example.proyectoreciclame.Dto.NormativaDTO;
import com.example.proyectoreciclame.Dto.NormativaDetalleDTO;
import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Entity.HistorialLectura;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.Repository.RegistroDescargaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.CitaService;
import com.example.proyectoreciclame.Service.HistorialLecturaService;
import com.example.proyectoreciclame.Service.NormativaService;
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

    @Autowired
    private RegistroDescargaRepository registroDescargaRepository;

    @Autowired
    private HistorialLecturaService historialLecturaService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

        // Metrics for visualizador
        long totalEstudios = estudioRepository.countByEliminadoEnIsNull();
        LocalDate limiteRecientes = LocalDate.now().minusDays(30);
        long estudiosRecientes = estudioRepository.countByFechaPublicacionAfterAndEliminadoEnIsNull(limiteRecientes);
        String anioMasActivo = resolveAnioMasActivo();

        model.addAttribute("totalEstudios", totalEstudios);
        model.addAttribute("estudiosRecientes", estudiosRecientes);
        model.addAttribute("anioMasActivo", anioMasActivo);

        return "visualizador/estudios";
    }

    @GetMapping("/estudios/{id}")
    public String verEstudio(@PathVariable Long id, Model model) {
        Estudio estudio = estudioRepository.findById(id).orElse(null);

        if (estudio == null) {
            return "redirect:/visualizador/estudios";
        }

        Long idUsuario = getIdUsuarioActual();
        if (idUsuario != null) {
            historialLecturaService.registrarOActualizar(idUsuario, "ESTUDIO", id, estudio.getTitulo());
        }

        model.addAttribute("estudio", estudio);
        model.addAttribute("citas", citaService.generarCitasEstudio(id));
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

        // Calculate metrics for donut chart
        long countEc = normativasDTO.stream()
                .filter(n -> n.categorias() != null && n.categorias().stream()
                        .anyMatch(c -> c.equalsIgnoreCase("Economía circular")))
                .count();
        long countGr = normativasDTO.stream()
                .filter(n -> n.categorias() != null && n.categorias().stream()
                        .anyMatch(c -> c.equalsIgnoreCase("Gestión de residuos")))
                .count();
        long countEe = normativasDTO.stream()
                .filter(n -> n.categorias() != null && n.categorias().stream()
                        .anyMatch(c -> c.equalsIgnoreCase("Envases y Embalajes")))
                .count();
        long countRep = normativasDTO.stream()
                .filter(n -> n.categorias() != null && n.categorias().stream()
                        .anyMatch(c -> c.equalsIgnoreCase("Ley REP") || c.equalsIgnoreCase("Responsabilidad Extendida")))
                .count();
        long countOtro = normativasDTO.stream()
                .filter(n -> n.categorias() != null && n.categorias().stream()
                        .anyMatch(c -> c.equalsIgnoreCase("Otro")))
                .count();

        double pctEc = totalNormas > 0 ? (countEc * 100.0) / totalNormas : 0.0;
        double pctGr = totalNormas > 0 ? (countGr * 100.0) / totalNormas : 0.0;
        double pctEe = totalNormas > 0 ? (countEe * 100.0) / totalNormas : 0.0;
        double pctRep = totalNormas > 0 ? (countRep * 100.0) / totalNormas : 0.0;
        double pctOtro = totalNormas > 0 ? (countOtro * 100.0) / totalNormas : 0.0;

        long totalCategorias = countEc + countGr + countEe + countRep + countOtro;
        double donutPctEc = totalCategorias > 0 ? (countEc * 100.0) / totalCategorias : 0.0;
        double donutPctGr = totalCategorias > 0 ? (countGr * 100.0) / totalCategorias : 0.0;
        double donutPctEe = totalCategorias > 0 ? (countEe * 100.0) / totalCategorias : 0.0;
        double donutPctRep = totalCategorias > 0 ? (countRep * 100.0) / totalCategorias : 0.0;
        double donutPctOtro = totalCategorias > 0 ? (countOtro * 100.0) / totalCategorias : 0.0;

        double angleEc = donutPctEc * 3.6;
        double angleGr = donutPctGr * 3.6;
        double angleEe = donutPctEe * 3.6;
        double angleRep = donutPctRep * 3.6;
        double angleOtro = donutPctOtro * 3.6;

        double startEc = 0.0;
        double startGr = startEc + angleEc;
        double startEe = startGr + angleGr;
        double startRep = startEe + angleEe;
        double startOtro = startRep + angleRep;

        double circumference = 2 * Math.PI * 45;
        String dashEc = (donutPctEc / 100.0 * circumference) + " " + (circumference - (donutPctEc / 100.0 * circumference));
        String dashGr = (donutPctGr / 100.0 * circumference) + " " + (circumference - (donutPctGr / 100.0 * circumference));
        String dashEe = (donutPctEe / 100.0 * circumference) + " " + (circumference - (donutPctEe / 100.0 * circumference));
        String dashRep = (donutPctRep / 100.0 * circumference) + " " + (circumference - (donutPctRep / 100.0 * circumference));
        String dashOtro = (donutPctOtro / 100.0 * circumference) + " " + (circumference - (donutPctOtro / 100.0 * circumference));

        // Calculate metrics for stacked bars
        long nacVigente = normativasDTO.stream().filter(n -> "NACIONAL".equalsIgnoreCase(n.alcance()) && "VIGENTE".equalsIgnoreCase(n.estado())).count();
        long nacPublicada = normativasDTO.stream().filter(n -> "NACIONAL".equalsIgnoreCase(n.alcance()) && "PUBLICADA".equalsIgnoreCase(n.estado())).count();
        long nacConsulta = normativasDTO.stream().filter(n -> "NACIONAL".equalsIgnoreCase(n.alcance()) && "CONSULTA PUBLICA".equalsIgnoreCase(n.estado())).count();
        long nacBorrador = normativasDTO.stream().filter(n -> "NACIONAL".equalsIgnoreCase(n.alcance()) && "BORRADOR EN PROCESO".equalsIgnoreCase(n.estado())).count();
        long nacDerogada = normativasDTO.stream().filter(n -> "NACIONAL".equalsIgnoreCase(n.alcance()) && "DEROGADA".equalsIgnoreCase(n.estado())).count();
        long nacTotal = nacVigente + nacPublicada + nacConsulta + nacBorrador + nacDerogada;

        long intVigente = normativasDTO.stream().filter(n -> "INTERNACIONAL".equalsIgnoreCase(n.alcance()) && "VIGENTE".equalsIgnoreCase(n.estado())).count();
        long intPublicada = normativasDTO.stream().filter(n -> "INTERNACIONAL".equalsIgnoreCase(n.alcance()) && "PUBLICADA".equalsIgnoreCase(n.estado())).count();
        long intConsulta = normativasDTO.stream().filter(n -> "INTERNACIONAL".equalsIgnoreCase(n.alcance()) && "CONSULTA PUBLICA".equalsIgnoreCase(n.estado())).count();
        long intBorrador = normativasDTO.stream().filter(n -> "INTERNACIONAL".equalsIgnoreCase(n.alcance()) && "BORRADOR EN PROCESO".equalsIgnoreCase(n.estado())).count();
        long intDerogada = normativasDTO.stream().filter(n -> "INTERNACIONAL".equalsIgnoreCase(n.alcance()) && "DEROGADA".equalsIgnoreCase(n.estado())).count();
        long intTotal = intVigente + intPublicada + intConsulta + intBorrador + intDerogada;

        long gratisNac = normativasDTO.stream().filter(n -> "GRATIS".equalsIgnoreCase(n.acceso()) && "NACIONAL".equalsIgnoreCase(n.alcance())).count();
        long gratisInt = normativasDTO.stream().filter(n -> "GRATIS".equalsIgnoreCase(n.acceso()) && "INTERNACIONAL".equalsIgnoreCase(n.alcance())).count();
        long gratisTotal = gratisNac + gratisInt;

        long pagoNac = normativasDTO.stream().filter(n -> "PAGO".equalsIgnoreCase(n.acceso()) && "NACIONAL".equalsIgnoreCase(n.alcance())).count();
        long pagoInt = normativasDTO.stream().filter(n -> "PAGO".equalsIgnoreCase(n.acceso()) && "INTERNACIONAL".equalsIgnoreCase(n.alcance())).count();
        long pagoTotal = pagoNac + pagoInt;

        model.addAttribute("normativas", normativasDTO);
        model.addAttribute("currentPage", "visualizadorRepoNormativo");
        model.addAttribute("totalNormas", totalNormas);
        model.addAttribute("countEc", countEc);
        model.addAttribute("countGr", countGr);
        model.addAttribute("countEe", countEe);
        model.addAttribute("countRep", countRep);
        model.addAttribute("countOtro", countOtro);
        model.addAttribute("pctEc", pctEc);
        model.addAttribute("pctGr", pctGr);
        model.addAttribute("pctEe", pctEe);
        model.addAttribute("pctRep", pctRep);
        model.addAttribute("pctOtro", pctOtro);
        model.addAttribute("angleEc", angleEc);
        model.addAttribute("angleGr", angleGr);
        model.addAttribute("angleEe", angleEe);
        model.addAttribute("angleRep", angleRep);
        model.addAttribute("angleOtro", angleOtro);
        model.addAttribute("startEc", startEc);
        model.addAttribute("startGr", startGr);
        model.addAttribute("startEe", startEe);
        model.addAttribute("startRep", startRep);
        model.addAttribute("startOtro", startOtro);
        model.addAttribute("dashEc", dashEc);
        model.addAttribute("dashGr", dashGr);
        model.addAttribute("dashEe", dashEe);
        model.addAttribute("dashRep", dashRep);
        model.addAttribute("dashOtro", dashOtro);

        model.addAttribute("nacVigente", nacVigente);
        model.addAttribute("nacPublicada", nacPublicada);
        model.addAttribute("nacConsulta", nacConsulta);
        model.addAttribute("nacBorrador", nacBorrador);
        model.addAttribute("nacDerogada", nacDerogada);
        model.addAttribute("nacTotal", nacTotal);

        model.addAttribute("intVigente", intVigente);
        model.addAttribute("intPublicada", intPublicada);
        model.addAttribute("intConsulta", intConsulta);
        model.addAttribute("intBorrador", intBorrador);
        model.addAttribute("intDerogada", intDerogada);
        model.addAttribute("intTotal", intTotal);

        model.addAttribute("gratisNac", gratisNac);
        model.addAttribute("gratisInt", gratisInt);
        model.addAttribute("gratisTotal", gratisTotal);

        model.addAttribute("pagoNac", pagoNac);
        model.addAttribute("pagoInt", pagoInt);
        model.addAttribute("pagoTotal", pagoTotal);

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

        Normativa normativa = detalle.getNormativa();
        Long idUsuario = getIdUsuarioActual();
        if (idUsuario != null) {
            historialLecturaService.registrarOActualizar(idUsuario, "NORMATIVA", id, normativa.getTitulo());
        }

        model.addAttribute("detalle", detalle);
        model.addAttribute("normativa", normativa);
        model.addAttribute("citas", citaService.generarCitasNormativa(id));
        model.addAttribute("currentPage", "visualizadorRepoNormativo");

        return "visualizador/visualizadorNormativa";
    }

    @GetMapping("/mis-lecturas")
    public String misLecturas(Model model) {
        Long idUsuario = getIdUsuarioActual();
        if (idUsuario == null) return "redirect:/login";

        List<HistorialLectura> lecturas = historialLecturaService.listarRecientes(idUsuario, 15);
        long totalEstudios = lecturas.stream().filter(l -> "ESTUDIO".equals(l.getTipoDocumento())).count();
        long totalNormativas = lecturas.stream().filter(l -> "NORMATIVA".equals(l.getTipoDocumento())).count();

        model.addAttribute("lecturas", lecturas);
        model.addAttribute("totalEstudios", totalEstudios);
        model.addAttribute("totalNormativas", totalNormativas);
        model.addAttribute("currentPage", "misLecturas");
        return "visualizador/misLecturas";
    }

    private Long getIdUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return null;
        return usuarioRepository.findByCorreoWithRol(auth.getName())
                .map(u -> u.getIdUsuario())
                .orElse(null);
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

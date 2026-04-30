package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.NovedadDTO;
import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.util.DateUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
public class DashboardController {

    private final NormativaRepository normativaRepository;
    private final EstudioRepository estudioRepository;
    private final DateUtil dateUtil;

    public DashboardController(NormativaRepository normativaRepository,
                               EstudioRepository estudioRepository,
                               DateUtil dateUtil) {
        this.normativaRepository = normativaRepository;
        this.estudioRepository = estudioRepository;
        this.dateUtil = dateUtil;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {

        model.addAttribute("currentSection", "admin-dashboard");

        // CARD 1: Normativas vigentes
        long normativasVigentes = normativaRepository.countByEstadoAndEliminadoEnIsNull(
                Normativa.EstadoNormativa.VIGENTE
        );

        long normativasVigentesNacional = normativaRepository.countByEstadoAndAlcanceAndEliminadoEnIsNull(
                Normativa.EstadoNormativa.VIGENTE,
                Normativa.AlcanceNormativa.NACIONAL
        );

        long normativasVigentesInternacional = normativaRepository.countByEstadoAndAlcanceAndEliminadoEnIsNull(
                Normativa.EstadoNormativa.VIGENTE,
                Normativa.AlcanceNormativa.INTERNACIONAL
        );

        // CARD 2: Estudios vigentes
        long estudiosVigentes = estudioRepository.countByEstadoAndEliminadoEnIsNull(
                Estudio.EstadoEstudio.VIGENTE
        );

        long estudiosPdf = estudioRepository.countByEstadoAndFormatoAndEliminadoEnIsNull(
                Estudio.EstadoEstudio.VIGENTE,
                Estudio.FormatoEstudio.PDF
        );

        long estudiosPptx = estudioRepository.countByEstadoAndFormatoAndEliminadoEnIsNull(
                Estudio.EstadoEstudio.VIGENTE,
                Estudio.FormatoEstudio.PPTX
        );

        // CARD 3: Consulta pública
        long normativasConsulta = normativaRepository.countByEstadoAndEliminadoEnIsNull(
                Normativa.EstadoNormativa.CONSULTA_PUBLICA
        );

        long normativasConsultaNacional = normativaRepository.countByEstadoAndAlcanceAndEliminadoEnIsNull(
                Normativa.EstadoNormativa.CONSULTA_PUBLICA,
                Normativa.AlcanceNormativa.NACIONAL
        );

        long normativasConsultaInternacional = normativaRepository.countByEstadoAndAlcanceAndEliminadoEnIsNull(
                Normativa.EstadoNormativa.CONSULTA_PUBLICA,
                Normativa.AlcanceNormativa.INTERNACIONAL
        );

        // CARD 4: Normativas derogadas
        long normativasDerogadas = normativaRepository.countByEstadoAndEliminadoEnIsNull(
                Normativa.EstadoNormativa.DEROGADA
        );

        long normativasDerogadasNacional = normativaRepository.countByEstadoAndAlcanceAndEliminadoEnIsNull(
                Normativa.EstadoNormativa.DEROGADA,
                Normativa.AlcanceNormativa.NACIONAL
        );

        long normativasDerogadasInternacional = normativaRepository.countByEstadoAndAlcanceAndEliminadoEnIsNull(
                Normativa.EstadoNormativa.DEROGADA,
                Normativa.AlcanceNormativa.INTERNACIONAL
        );

        model.addAttribute("normativasVigentes", normativasVigentes);
        model.addAttribute("normativasVigentesNacional", normativasVigentesNacional);
        model.addAttribute("normativasVigentesInternacional", normativasVigentesInternacional);

        model.addAttribute("estudiosVigentes", estudiosVigentes);
        model.addAttribute("estudiosPdf", estudiosPdf);
        model.addAttribute("estudiosPptx", estudiosPptx);

        model.addAttribute("normativasConsulta", normativasConsulta);
        model.addAttribute("normativasConsultaNacional", normativasConsultaNacional);
        model.addAttribute("normativasConsultaInternacional", normativasConsultaInternacional);

        model.addAttribute("normativasDerogadas", normativasDerogadas);
        model.addAttribute("normativasDerogadasNacional", normativasDerogadasNacional);
        model.addAttribute("normativasDerogadasInternacional", normativasDerogadasInternacional);

        // Distribución por temática
        List<Normativa> normativas = normativaRepository.findAllNormativas();

        long countEe = normativas.stream()
                .filter(n -> n.getCategorias() != null && n.getCategorias().stream()
                        .anyMatch(c -> c.getNombre().equalsIgnoreCase("Envases y Embalajes")))
                .count();

        long countGr = normativas.stream()
                .filter(n -> n.getCategorias() != null && n.getCategorias().stream()
                        .anyMatch(c -> c.getNombre().equalsIgnoreCase("Gestión de residuos")))
                .count();

        long countRep = normativas.stream()
                .filter(n -> n.getCategorias() != null && n.getCategorias().stream()
                        .anyMatch(c -> c.getNombre().equalsIgnoreCase("Ley REP")
                                || c.getNombre().equalsIgnoreCase("Responsabilidad Extendida")))
                .count();

        long countOtro = normativas.stream()
                .filter(n -> n.getCategorias() != null && n.getCategorias().stream()
                        .anyMatch(c -> c.getNombre().equalsIgnoreCase("Otro")))
                .count();

        long totalNormas = normativas.size();

        double totalCategorias = countEe + countGr + countRep + countOtro;

        double pctEe = totalCategorias > 0 ? countEe * 100.0 / totalCategorias : 0;
        double pctGr = totalCategorias > 0 ? countGr * 100.0 / totalCategorias : 0;
        double pctRep = totalCategorias > 0 ? countRep * 100.0 / totalCategorias : 0;
        double pctOtro = totalCategorias > 0 ? countOtro * 100.0 / totalCategorias : 0;

        model.addAttribute("totalNormas", totalNormas);
        model.addAttribute("countEe", countEe);
        model.addAttribute("countGr", countGr);
        model.addAttribute("countRep", countRep);
        model.addAttribute("countOtro", countOtro);

        model.addAttribute("dashEe", pctEe + " 100");
        model.addAttribute("dashGr", pctGr + " 100");
        model.addAttribute("dashRep", pctRep + " 100");
        model.addAttribute("dashOtro", pctOtro + " 100");

        model.addAttribute("offsetGr", -pctEe);
        model.addAttribute("offsetRep", -(pctEe + pctGr));
        model.addAttribute("offsetOtro", -(pctEe + pctGr + pctRep));

        // Últimas actualizaciones
        List<NovedadDTO> novedades = new ArrayList<>();

        normativaRepository.findTop3ByEliminadoEnIsNullOrderByFechaActualizacionDesc()
                .forEach(n -> novedades.add(new NovedadDTO(
                        "Normativa",
                        n.getTitulo(),
                        n.getFechaCreacion() != null && n.getFechaCreacion().equals(n.getFechaActualizacion())
                                ? "AÑADIDO"
                                : "MODIFICADO",
                        "NOR",
                        "#006d37",
                        n.getFechaActualizacion(),
                        dateUtil.formatRelative(n.getFechaActualizacion())
                )));

        estudioRepository.findTop3ByEliminadoEnIsNullOrderByFechaActualizacionDesc()
                .forEach(e -> novedades.add(new NovedadDTO(
                        "Estudio",
                        e.getTitulo(),
                        e.getFechaCreacion() != null && e.getFechaCreacion().equals(e.getFechaActualizacion())
                                ? "AÑADIDO"
                                : "MODIFICADO",
                        "EST",
                        "#378ADD",
                        e.getFechaActualizacion(),
                        dateUtil.formatRelative(e.getFechaActualizacion())
                )));

        novedades.sort(Comparator.comparing(NovedadDTO::getFecha).reversed());

        model.addAttribute("ultimasActualizaciones",
                novedades.stream().limit(3).toList());

        return "admin/dashboard_admin";
    }

    @GetMapping("/admin/estudio_new")
    public String nuevoEstudio(Model model) {
        model.addAttribute("currentSection", "admin-estudios");
        return "admin/estudio_new";
    }
}
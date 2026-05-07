package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.Repository.RegistroDescargaRepository;
import com.example.proyectoreciclame.Repository.RegistroSesionRepository;
import com.example.proyectoreciclame.util.DateUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/reportes")
public class AdminReporteController {

    private final RegistroSesionRepository registroSesionRepository;
    private final RegistroDescargaRepository registroDescargaRepository;

    // constructor...
    public AdminReporteController(RegistroSesionRepository registroSesionRepository,
                                  RegistroDescargaRepository registroDescargaRepository) {
        this.registroSesionRepository = registroSesionRepository;
        this.registroDescargaRepository = registroDescargaRepository;
    }

    @GetMapping
    public String reportes(Model model) {
        LocalDateTime inicioDia   = LocalDate.now().atStartOfDay();
        LocalDateTime inicioSemana = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime inicio30   = LocalDate.now().minusDays(29).atStartOfDay();

        // KPIs
        model.addAttribute("ingresosHoy",     registroSesionRepository.countByFechaInicioAfter(inicioDia));
        model.addAttribute("ingresosSemana",  registroSesionRepository.countByFechaInicioAfter(inicioSemana));
        model.addAttribute("totalDescargas",  registroDescargaRepository.count());
        model.addAttribute("usuariosActivos", registroSesionRepository.countUsuariosActivosDesde(inicio30));

        // Gráfico de barras — últimos 7 días
        List<Object[]> porDia = registroSesionRepository.contarSesionesPorDia(inicioSemana);
        List<String> etiquetas = porDia.stream()
                .map(r -> LocalDate.parse(r[0].toString())
                        .getDayOfWeek()
                        .getDisplayName(java.time.format.TextStyle.SHORT, new java.util.Locale("es", "PE")))
                .toList();
        model.addAttribute("diasLabels", etiquetas);
        model.addAttribute("diasValores", porDia.stream().map(r -> r[1].toString()).toList());

        // Dona descargas
        model.addAttribute("descargasEstudio",   registroDescargaRepository.countByTipoDocumento("ESTUDIO"));
        model.addAttribute("descargasNormativa", registroDescargaRepository.countByTipoDocumento("NORMATIVA"));

        // Top 5 documentos
        model.addAttribute("topDocumentos", registroDescargaRepository.topDocumentosMasDescargados());

        model.addAttribute("currentSection", "admin-reportes");

        // En el controlador:
        model.addAttribute("descargasSocio",       registroDescargaRepository.countByRolUsuario("SOCIO"));
        model.addAttribute("descargasVisualizador", registroDescargaRepository.countByRolUsuario("VISUALIZADOR"));
        return "admin/reportes";
    }
}

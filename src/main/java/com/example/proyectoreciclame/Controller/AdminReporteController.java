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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Object> serieIngresos = construirSerieIngresos(7);
        model.addAttribute("diasLabels", serieIngresos.get("diasLabels"));
        model.addAttribute("diasValores", serieIngresos.get("diasValores"));

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

    @GetMapping("/datos-ingresos")
    @ResponseBody
    public Map<String, Object> obtenerDatosIngresos(@RequestParam(defaultValue = "7") int dias) {
        return construirSerieIngresos(dias);
    }

    /**
     * Arma la serie de ingresos por día incluyendo TODOS los días del rango
     * (aunque tengan 0 sesiones), con etiqueta de dos líneas: día de semana + fecha corta.
     */
    private Map<String, Object> construirSerieIngresos(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioFecha = hoy.minusDays(dias - 1);
        LocalDateTime inicio = inicioFecha.atStartOfDay();

        List<Object[]> porDia = registroSesionRepository.contarSesionesPorDia(inicio);
        Map<LocalDate, Integer> conteoPorDia = new HashMap<>();
        for (Object[] r : porDia) {
            conteoPorDia.put(LocalDate.parse(r[0].toString()), Integer.parseInt(r[1].toString()));
        }

        java.util.Locale localeEs = new java.util.Locale("es", "PE");
        java.time.format.DateTimeFormatter fechaCortaFmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

        List<List<String>> etiquetas = new java.util.ArrayList<>();
        List<Integer> valores = new java.util.ArrayList<>();

        for (LocalDate fecha = inicioFecha; !fecha.isAfter(hoy); fecha = fecha.plusDays(1)) {
            String diaSemana = fecha.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, localeEs);
            etiquetas.add(List.of(diaSemana, fecha.format(fechaCortaFmt)));
            valores.add(conteoPorDia.getOrDefault(fecha, 0));
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("diasLabels", etiquetas);
        resultado.put("diasValores", valores);
        return resultado;
    }
}

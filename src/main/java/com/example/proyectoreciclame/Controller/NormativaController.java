package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.NormativaDTO;
import com.example.proyectoreciclame.Dto.NormativaDetalleDTO;
import com.example.proyectoreciclame.Entity.Categoria;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Repository.CategoriaRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.CitaService;
import com.example.proyectoreciclame.Service.HistorialLecturaService;
import com.example.proyectoreciclame.Service.MiEspacioService;
import com.example.proyectoreciclame.Service.NormativaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private NormativaService normativaService;

    @Autowired
    private MiEspacioService miEspacioService;

    @Autowired
    private HistorialLecturaService historialLecturaService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String listarNormativas(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "yearSelect") Integer anio,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd,
            @RequestParam(required = false) List<String> categoria,
            @RequestParam(required = false) List<String> estado,
            @RequestParam(required = false) List<String> acceso,
            @RequestParam(required = false) List<String> alcance,
            @RequestParam(required = false) List<String> obligatoriedad,
            Model model) {

        List<Normativa> normativas;

        boolean hasCategoriaParam = categoria != null && !categoria.isEmpty();
        boolean hasEstadoParam = estado != null && !estado.isEmpty();
        boolean hasAccesoParam = acceso != null && !acceso.isEmpty();
        boolean hasAlcanceParam = alcance != null && !alcance.isEmpty();
        boolean hasObligatoriedadParam = obligatoriedad != null && !obligatoriedad.isEmpty();

        if (search != null && !search.trim().isEmpty()) {
            normativas = normativaRepository.findByKeyword(search);
        } else if (anio != null || hasEstadoParam || hasAccesoParam || hasAlcanceParam || hasCategoriaParam || hasObligatoriedadParam || dateStart != null || dateEnd != null) {
            boolean hasAnio = anio != null;
            boolean hasEstados = hasEstadoParam;
            boolean hasAccesos = hasAccesoParam;
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

            java.util.List<String> queryEstados = java.util.List.of();
            if (hasEstados) {
                queryEstados = estado.stream().map(e -> e.replace(" ", "_")).collect(Collectors.toList());
            }

            normativas = normativaRepository.findWithAdvancedFilters(hasAnio, anio, fechaInicio, fechaFin, hasEstados, queryEstados, hasAccesos,
                    acceso, hasAlcance, alcance, hasObligatoriedad, obligatoriedad, hasCategoria, categoria);
        } else {
            normativas = normativaRepository.findAllNormativas();
        }

        normativas = normativas.stream()
                .sorted(java.util.Comparator
                        .comparing(
                                Normativa::getIdNormativa,
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
                        )
                        .thenComparing(
                                Normativa::getFechaCreacion,
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
                        ))
                .toList();

        List<NormativaDTO> normativasDTO = normativas.stream()
                .map(NormativaDTO::fromEntity)
                .collect(Collectors.toList());

        // ✅ Usa TODOS los datos

// Lista general SOLO para gráficos superiores
        List<NormativaDTO> normativasGraficoDTO = normativaRepository.findAllNormativas().stream()
                .map(NormativaDTO::fromEntity)
                .collect(Collectors.toList());

        int totalNormas = normativasGraficoDTO.size();



        long countEc = normativasGraficoDTO.stream()
                .filter(n -> n.categorias() != null && n.categorias().stream()
                        .anyMatch(c -> c.equalsIgnoreCase("Economía circular")))
                .count();
        long countGr = normativasGraficoDTO.stream()
                .filter(n -> n.categorias() != null && n.categorias().stream()
                        .anyMatch(c -> c.equalsIgnoreCase("Gestión de residuos")))
                .count();
        long countEe = normativasGraficoDTO.stream()
                .filter(n -> n.categorias() != null && n.categorias().stream()
                        .anyMatch(c -> c.equalsIgnoreCase("Envases y Embalajes")))
                .count();
        long countRep = normativasGraficoDTO.stream()
                .filter(n -> n.categorias() != null && n.categorias().stream()
                        .anyMatch(c -> c.equalsIgnoreCase("Ley REP") || c.equalsIgnoreCase("Responsabilidad Extendida")))
                .count();
        long countOtro = normativasGraficoDTO.stream()
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

        java.util.List<Normativa> normativasGraficos = normativaRepository.findAllNormativas();
        java.util.Map<Integer, java.util.Map<String, Object>> statsPorCategoria = new java.util.LinkedHashMap<>();

        for (Normativa normativa : normativasGraficos) {
            if (normativa.getCategorias() == null || normativa.getCategorias().isEmpty()) {
                continue;
            }

            for (Categoria categoriaNormativa : normativa.getCategorias()) {
                if (categoriaNormativa == null || categoriaNormativa.getIdCategoria() == null || esCategoriaOtro(categoriaNormativa)) {
                    continue;
                }

                java.util.Map<String, Object> stat = statsPorCategoria.computeIfAbsent(
                        categoriaNormativa.getIdCategoria(),
                        id -> crearCategoriaStat(categoriaNormativa)
                );
                stat.put("count", ((Long) stat.get("count")) + 1L);
            }
        }

        java.util.List<java.util.Map<String, Object>> categoriaStats = new java.util.ArrayList<>(statsPorCategoria.values());
        categoriaStats.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));

        long totalCategoriasDinamicas = categoriaStats.stream()
                .mapToLong(stat -> (Long) stat.get("count"))
                .sum();

        String[] coloresCategorias = {"#006c46", "#3b82f6", "#f49e0b", "#ba1a1a", "#7c3aed", "#0891b2", "#db2777", "#4b5563"};
        for (int i = 0; i < categoriaStats.size(); i++) {
            java.util.Map<String, Object> stat = categoriaStats.get(i);
            long count = (Long) stat.get("count");
            double pct = totalCategoriasDinamicas > 0 ? (count * 100.0 / totalCategoriasDinamicas) : 0;
            String color = (String) stat.get("color");

            if (color == null || color.isBlank()) {
                color = coloresCategorias[i % coloresCategorias.length];
            }

            stat.put("pct", pct);
            stat.put("color", color);
        }

        java.util.List<java.util.Map<String, Object>> categoriaStatsGrafico = new java.util.ArrayList<>();
        int maxSegmentosGrafico = 8;
        int maxCategoriasVisibles = maxSegmentosGrafico - 1;

        if (categoriaStats.size() > maxSegmentosGrafico) {
            for (int i = 0; i < maxCategoriasVisibles; i++) {
                categoriaStatsGrafico.add(new java.util.LinkedHashMap<>(categoriaStats.get(i)));
            }

            long countRestantes = categoriaStats.subList(maxCategoriasVisibles, categoriaStats.size()).stream()
                    .mapToLong(stat -> (Long) stat.get("count"))
                    .sum();

            java.util.Map<String, Object> restantes = new java.util.LinkedHashMap<>();
            restantes.put("id", -1);
            restantes.put("nombre", "Mas categorias");
            restantes.put("codigo", "+" + (categoriaStats.size() - maxCategoriasVisibles));
            restantes.put("color", "#64748b");
            restantes.put("count", countRestantes);
            categoriaStatsGrafico.add(restantes);
        } else {
            categoriaStats.forEach(stat -> categoriaStatsGrafico.add(new java.util.LinkedHashMap<>(stat)));
        }

        double startGrafico = -90;
        for (java.util.Map<String, Object> stat : categoriaStatsGrafico) {
            long count = (Long) stat.get("count");
            double pct = totalCategoriasDinamicas > 0 ? (count * 100.0 / totalCategoriasDinamicas) : 0;
            double dash = totalCategoriasDinamicas > 0 ? (count * circumference / totalCategoriasDinamicas) : 0;
            stat.put("pct", pct);
            stat.put("dash", dash + " " + circumference);
            stat.put("start", startGrafico);
            startGrafico += pct * 3.6;
        }

        // CHarts logic

        long nacVigente = normativasGraficoDTO.stream().filter(n -> "NACIONAL".equalsIgnoreCase(n.alcance()) && "VIGENTE".equalsIgnoreCase(n.estado())).count();
        long nacPublicada = normativasGraficoDTO.stream().filter(n -> "NACIONAL".equalsIgnoreCase(n.alcance()) && "PUBLICADA".equalsIgnoreCase(n.estado())).count();
        long nacConsulta = normativasGraficoDTO.stream().filter(n -> "NACIONAL".equalsIgnoreCase(n.alcance()) && "CONSULTA PUBLICA".equalsIgnoreCase(n.estado())).count();
        long nacBorrador = normativasGraficoDTO.stream().filter(n -> "NACIONAL".equalsIgnoreCase(n.alcance()) && "BORRADOR EN PROCESO".equalsIgnoreCase(n.estado())).count();
        long nacDerogada = normativasGraficoDTO.stream().filter(n -> "NACIONAL".equalsIgnoreCase(n.alcance()) && "DEROGADA".equalsIgnoreCase(n.estado())).count();
        long nacTotal = nacVigente + nacPublicada + nacConsulta + nacBorrador + nacDerogada;

        long intVigente = normativasGraficoDTO.stream().filter(n -> "INTERNACIONAL".equalsIgnoreCase(n.alcance()) && "VIGENTE".equalsIgnoreCase(n.estado())).count();
        long intPublicada = normativasGraficoDTO.stream().filter(n -> "INTERNACIONAL".equalsIgnoreCase(n.alcance()) && "PUBLICADA".equalsIgnoreCase(n.estado())).count();
        long intConsulta = normativasGraficoDTO.stream().filter(n -> "INTERNACIONAL".equalsIgnoreCase(n.alcance()) && "CONSULTA PUBLICA".equalsIgnoreCase(n.estado())).count();
        long intBorrador = normativasGraficoDTO.stream().filter(n -> "INTERNACIONAL".equalsIgnoreCase(n.alcance()) && "BORRADOR EN PROCESO".equalsIgnoreCase(n.estado())).count();
        long intDerogada = normativasGraficoDTO.stream().filter(n -> "INTERNACIONAL".equalsIgnoreCase(n.alcance()) && "DEROGADA".equalsIgnoreCase(n.estado())).count();
        long intTotal = intVigente + intPublicada + intConsulta + intBorrador + intDerogada;

        long gratisNac = normativasGraficoDTO.stream().filter(n -> "GRATIS".equalsIgnoreCase(n.acceso()) && "NACIONAL".equalsIgnoreCase(n.alcance())).count();
        long gratisInt = normativasGraficoDTO.stream().filter(n -> "GRATIS".equalsIgnoreCase(n.acceso()) && "INTERNACIONAL".equalsIgnoreCase(n.alcance())).count();
        long gratisTotal = gratisNac + gratisInt;

        long pagoNac = normativasGraficoDTO.stream().filter(n -> "PAGO".equalsIgnoreCase(n.acceso()) && "NACIONAL".equalsIgnoreCase(n.alcance())).count();
        long pagoInt = normativasGraficoDTO.stream().filter(n -> "PAGO".equalsIgnoreCase(n.acceso()) && "INTERNACIONAL".equalsIgnoreCase(n.alcance())).count();
        long pagoTotal = pagoNac + pagoInt;

        model.addAttribute("normativas", normativasDTO);
        model.addAttribute("currentPage", "repoNormativo");
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
        model.addAttribute("categoriaStatsGrafico", categoriaStatsGrafico);
        model.addAttribute("categoriaStats", categoriaStats);
        model.addAttribute("totalCategoriasDinamicas", totalCategoriasDinamicas);

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

        // Return filter params so we can retain them in the UI
        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedYear", anio);
        model.addAttribute("dateStart", dateStart);
        model.addAttribute("dateEnd", dateEnd);
        model.addAttribute("selectedCategorias", categoria);
        model.addAttribute("selectedEstados", estado);
        model.addAttribute("selectedAccesos", acceso);
        model.addAttribute("selectedAlcances", alcance);
        model.addAttribute("selectedObligatoriedades", obligatoriedad);
        model.addAttribute("categoriasNormativa",
                categoriaRepository.findByTipoAndEstadoTrue(Categoria.TipoCategoria.NORMATIVA));

        return "socio/repoNormativo";
    }

    @GetMapping("/{id}")
    public String verNormativa(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        NormativaDetalleDTO detalle = normativaService.obtenerDetalleConContexto(id);
        if (detalle == null || detalle.getNormativa() == null) {
            return "redirect:/normativas";
        }

        Normativa normativa = detalle.getNormativa();
        model.addAttribute("detalle", detalle);
        model.addAttribute("normativa", normativa);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean esVisualizador = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VISUALIZADOR"));
        boolean esSocio = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SOCIO"));

        Long idUsuario = resolveIdUsuario(authentication);

        if (esVisualizador) {
            model.addAttribute("currentPage", "visualizadorRepoNormativo");
            if (idUsuario != null) {
                historialLecturaService.registrarOActualizar(idUsuario, "NORMATIVA", id, normativa.getTitulo());
            }
            model.addAttribute("citas", citaService.generarCitasNormativa(id));
            if (normativa.getAcceso() == Normativa.AccesoNormativa.PAGO) {
                return "visualizador/visualizadorNormativaPago";
            }
            return "visualizador/visualizadorNormativa";
        } else if (esSocio && idUsuario != null) {
            model.addAttribute("currentPage", "repoNormativo");
            model.addAttribute("estaGuardado", miEspacioService.estaGuardado(idUsuario, "NORMATIVA", id));
            if (normativa.getAcceso() == Normativa.AccesoNormativa.PAGO) {
                return "socio/visualizadorNormativaPago";
            }
            return "socio/visualizadorNormativa";
        } else if (normativa.getAcceso() == Normativa.AccesoNormativa.PAGO) {
            model.addAttribute("currentPage", "repoNormativo");
            return "socio/visualizadorNormativaPago";
        } else {
            model.addAttribute("currentPage", "repoNormativo");
            return "socio/visualizadorNormativa";
        }
    }

    private Long resolveIdUsuario(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) return null;
        return usuarioRepository.findByCorreoWithRol(authentication.getName())
                .map(u -> u.getIdUsuario()).orElse(null);
    }

    private boolean esCategoriaOtro(Categoria categoria) {
        if (categoria == null) {
            return false;
        }

        String nombre = categoria.getNombre();
        String codigo = categoria.getCodigo();
        return (nombre != null && nombre.equalsIgnoreCase("Otro"))
                || (codigo != null && codigo.equalsIgnoreCase("OTRO"));
    }

    private java.util.Map<String, Object> crearCategoriaStat(Categoria categoria) {
        java.util.Map<String, Object> stat = new java.util.LinkedHashMap<>();
        String nombre = categoria.getNombre() != null ? categoria.getNombre() : "Categoria";
        String codigo = categoria.getCodigo();
        String color = categoria.getColorHex();

        stat.put("id", categoria.getIdCategoria());
        stat.put("nombre", nombre);
        stat.put("codigo", codigo != null && !codigo.isBlank() ? codigo : normalizarCodigoCategoria(nombre));
        stat.put("color", color != null && color.matches("^#[0-9A-Fa-f]{6}$") ? color : null);
        stat.put("count", 0L);
        return stat;
    }

    private String normalizarCodigoCategoria(String nombre) {
        String nombreBase = nombre != null ? nombre : "";
        String generado = java.util.Arrays.stream(nombreBase.split("\\s+"))
                .filter(s -> !s.isBlank())
                .limit(3)
                .map(s -> s.substring(0, 1).toUpperCase())
                .collect(java.util.stream.Collectors.joining());

        return generado.isBlank() ? "CAT" : generado;
    }
}

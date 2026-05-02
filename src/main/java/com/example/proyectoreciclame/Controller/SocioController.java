package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.NovedadDTO;
import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Entity.RegistroDescarga;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.*;
import com.example.proyectoreciclame.util.DateUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequestMapping("/socio")
public class SocioController {

    private final UsuarioRepository usuarioRepository;
    private final RegistroDescargaRepository registroDescargaRepository;
    private final RegistroSesionRepository registroSesionRepository;
    private final NotificacionRepository notificacionRepository;
    private final EstudioRepository estudioRepository;
    private final NormativaRepository normativaRepository;
    private final DateUtil dateUtil;

    public SocioController(UsuarioRepository usuarioRepository,
                           RegistroDescargaRepository registroDescargaRepository,
                           RegistroSesionRepository registroSesionRepository,
                           NotificacionRepository notificacionRepository,
                           EstudioRepository estudioRepository,
                           NormativaRepository normativaRepository,
                           DateUtil dateUtil) {
        this.usuarioRepository = usuarioRepository;
        this.registroDescargaRepository = registroDescargaRepository;
        this.registroSesionRepository = registroSesionRepository;
        this.notificacionRepository = notificacionRepository;
        this.estudioRepository = estudioRepository;
        this.normativaRepository = normativaRepository;
        this.dateUtil = dateUtil;
    }

    @GetMapping()
    public String panelPrincipal(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }
        
        String email = authentication.getName();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoWithRol(email);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            Long idUsuario = usuario.getIdUsuario();
            LocalDateTime now = LocalDateTime.now();
            Locale localeEs = new Locale("es", "PE");

            // Change 1: Mi Actividad Personal
            long totalDescargas = registroDescargaRepository.countByUsuarioIdUsuario(idUsuario);
            long descargasEstudios = registroDescargaRepository.countByUsuarioIdUsuarioAndTipoDocumento(idUsuario, "ESTUDIO");
            long descargasNormativas = registroDescargaRepository.countByUsuarioIdUsuarioAndTipoDocumento(idUsuario, "NORMATIVA");
            long totalDescargasTipo = descargasEstudios + descargasNormativas;
            int porcentajeEstudios = totalDescargasTipo > 0
                    ? (int) Math.round(descargasEstudios * 100.0 / totalDescargasTipo)
                    : 0;
            int porcentajeNormativas = totalDescargasTipo > 0
                    ? Math.max(0, 100 - porcentajeEstudios)
                    : 0;

            YearMonth currentMonth = YearMonth.now();
            LocalDateTime startOfCurrentMonth = currentMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfCurrentMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
            long descargasMesActual = registroDescargaRepository.countByUsuarioIdUsuarioAndFechaDescargaBetween(idUsuario, startOfCurrentMonth, endOfCurrentMonth);

            YearMonth previousMonth = currentMonth.minusMonths(1);
            LocalDateTime startOfPreviousMonth = previousMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfPreviousMonth = previousMonth.atEndOfMonth().atTime(23, 59, 59);
            long descargasMesAnterior = registroDescargaRepository.countByUsuarioIdUsuarioAndFechaDescargaBetween(idUsuario, startOfPreviousMonth, endOfPreviousMonth);

            long diferenciaDescargas = descargasMesActual - descargasMesAnterior;
            long novedadesSinLeer = notificacionRepository.countUnreadNovedadesByUsuario(idUsuario);
            String ultimaVisita = dateUtil.formatRelative(usuario.getUltimoAcceso());
            String mesActualTexto = currentMonth.getMonth().getDisplayName(TextStyle.FULL, localeEs);
            String mesAnteriorTexto = previousMonth.getMonth().getDisplayName(TextStyle.FULL, localeEs);

            long diasActivosMesActual = registroSesionRepository.countActiveDaysByUsuarioIdUsuarioBetween(idUsuario, startOfCurrentMonth, endOfCurrentMonth);
            int diasTranscurridosMesActual = LocalDate.now().getDayOfMonth();
            String promedioDescargasDiaActivo = formatDecimal(diasActivosMesActual > 0
                    ? (double) descargasMesActual / diasActivosMesActual
                    : 0.0);

            String diferenciaDescargasEtiqueta = buildDiferenciaEtiqueta(diferenciaDescargas);
            String diferenciaDescargasTexto = buildDiferenciaMesTexto(diferenciaDescargas, mesAnteriorTexto);
            String tendenciaDescargasTexto = buildTendenciaTexto(diferenciaDescargas);
            String tendenciaDescargasClase = buildTendenciaClase(diferenciaDescargas);

            long novedadesEstudios = notificacionRepository.countUnreadByUsuarioAndTipo(idUsuario, "ESTUDIO");
            long novedadesNormativas = notificacionRepository.countUnreadByUsuarioAndTipo(idUsuario, "NORMATIVA");
            LocalDateTime fechaMasAntiguaNovedad = notificacionRepository.findOldestUnreadFechaByUsuario(idUsuario);
            String novedadesAntiguedadTexto = fechaMasAntiguaNovedad != null
                    ? formatAntiguedad(fechaMasAntiguaNovedad, now)
                    : "Sin datos disponibles";
            String novedadesEstadoTexto = buildNovedadesEstadoTexto(fechaMasAntiguaNovedad, now, novedadesSinLeer);
            String novedadesEstadoClase = buildNovedadesEstadoClase(fechaMasAntiguaNovedad, now, novedadesSinLeer);

            LocalDateTime ultimoAcceso = usuario.getUltimoAcceso();
            String ultimaVisitaExacta = ultimoAcceso != null
                    ? ultimoAcceso.format(DateTimeFormatter.ofPattern("d MMM, h:mm a", localeEs))
                    : "Sin datos disponibles";
            boolean enLineaAhora = ultimoAcceso != null && Duration.between(ultimoAcceso, now).toMinutes() < 60;
            boolean tieneUltimoAcceso = ultimoAcceso != null;
            long visitasSemana = registroSesionRepository.countByUsuarioIdUsuarioAndFechaInicioBetween(idUsuario, now.minusDays(7), now);
            long visitasUltimas4Semanas = registroSesionRepository.countByUsuarioIdUsuarioAndFechaInicioBetween(idUsuario, now.minusDays(28), now);
            String promedioSemanalVisitas = formatDecimal(visitasUltimas4Semanas / 4.0);

            model.addAttribute("totalDescargas", totalDescargas);
            model.addAttribute("descargasMesActual", descargasMesActual);
            model.addAttribute("diferenciaDescargas", diferenciaDescargas);
            model.addAttribute("novedadesSinLeer", novedadesSinLeer);
            model.addAttribute("ultimaVisita", ultimaVisita);
            model.addAttribute("descargasEstudios", descargasEstudios);
            model.addAttribute("descargasNormativas", descargasNormativas);
            model.addAttribute("porcentajeEstudios", porcentajeEstudios);
            model.addAttribute("porcentajeNormativas", porcentajeNormativas);
            model.addAttribute("mesActualTexto", mesActualTexto);
            model.addAttribute("mesAnteriorTexto", mesAnteriorTexto);
            model.addAttribute("diasActivosMesActual", diasActivosMesActual);
            model.addAttribute("diasTranscurridosMesActual", diasTranscurridosMesActual);
            model.addAttribute("promedioDescargasDiaActivo", promedioDescargasDiaActivo);
            model.addAttribute("diferenciaDescargasEtiqueta", diferenciaDescargasEtiqueta);
            model.addAttribute("diferenciaDescargasTexto", diferenciaDescargasTexto);
            model.addAttribute("tendenciaDescargasTexto", tendenciaDescargasTexto);
            model.addAttribute("tendenciaDescargasClase", tendenciaDescargasClase);
            model.addAttribute("novedadesEstudios", novedadesEstudios);
            model.addAttribute("novedadesNormativas", novedadesNormativas);
            model.addAttribute("novedadesAntiguedadTexto", novedadesAntiguedadTexto);
            model.addAttribute("novedadesEstadoTexto", novedadesEstadoTexto);
            model.addAttribute("novedadesEstadoClase", novedadesEstadoClase);
            model.addAttribute("ultimaVisitaExacta", ultimaVisitaExacta);
            model.addAttribute("enLineaAhora", enLineaAhora);
            model.addAttribute("tieneUltimoAcceso", tieneUltimoAcceso);
            model.addAttribute("visitasSemana", visitasSemana);
            model.addAttribute("promedioSemanalVisitas", promedioSemanalVisitas);

            // Change 2: Novedades desde tu última visita
            LocalDateTime fechaUltimaVisita = usuario.getUltimoAcceso() != null ? usuario.getUltimoAcceso() : LocalDateTime.now().minusYears(1);
            List<NovedadDTO> novedades = new ArrayList<>();

            List<Estudio> estudiosNuevos = estudioRepository.findByFechaCreacionAfter(fechaUltimaVisita);
            List<Estudio> estudiosModificados = estudioRepository.findByFechaActualizacionAfterAndFechaCreacionBefore(fechaUltimaVisita, fechaUltimaVisita);
            List<Normativa> normativasNuevas = normativaRepository.findByFechaCreacionAfter(fechaUltimaVisita);
            List<Normativa> normativasModificadas = normativaRepository.findByFechaActualizacionAfterAndFechaCreacionBefore(fechaUltimaVisita, fechaUltimaVisita);

            estudiosNuevos.forEach(e -> novedades.add(new NovedadDTO("Estudio", e.getTitulo(), "Añadido", "EST", "#378ADD", e.getFechaCreacion(), dateUtil.formatRelative(e.getFechaCreacion()))));
            estudiosModificados.forEach(e -> novedades.add(new NovedadDTO("Estudio", e.getTitulo(), "Modificado", "EST", "#378ADD", e.getFechaActualizacion(), dateUtil.formatRelative(e.getFechaActualizacion()))));
            normativasNuevas.forEach(n -> {
                String cat = !n.getCategorias().isEmpty() ? n.getCategorias().get(0).getNombre().substring(0, 2) : "OT";
                String color = getColorForCategory(cat);
                novedades.add(new NovedadDTO("Normativa", n.getTitulo(), "Añadido", cat, color, n.getFechaCreacion(), dateUtil.formatRelative(n.getFechaCreacion())));
            });
            normativasModificadas.forEach(n -> {
                String cat = !n.getCategorias().isEmpty() ? n.getCategorias().get(0).getNombre().substring(0, 2) : "OT";
                String color = getColorForCategory(cat);
                novedades.add(new NovedadDTO("Normativa", n.getTitulo(), "Modificado", cat, color, n.getFechaActualizacion(), dateUtil.formatRelative(n.getFechaActualizacion())));
            });

            novedades.sort(Comparator.comparing(NovedadDTO::getFecha).reversed());
            model.addAttribute("novedades", novedades);

            // Change 3: Mis últimas descargas
            List<RegistroDescarga> ultimasDescargas = registroDescargaRepository.findTop3ByUsuarioIdUsuarioOrderByFechaDescargaDesc(idUsuario);
            model.addAttribute("ultimasDescargas", ultimasDescargas);
            model.addAttribute("dateUtil", dateUtil);
        }

        model.addAttribute("currentPage", "inicio");
        return "socio/panelPrincipal";
    }

    private String buildDiferenciaEtiqueta(long diferenciaDescargas) {
        if (diferenciaDescargas > 0) {
            return "+" + diferenciaDescargas + " vs mes ant.";
        }
        if (diferenciaDescargas < 0) {
            return diferenciaDescargas + " vs mes ant.";
        }
        return "Sin variación vs mes ant.";
    }

    private String buildDiferenciaMesTexto(long diferenciaDescargas, String mesAnteriorTexto) {
        if (diferenciaDescargas > 0) {
            return "+" + diferenciaDescargas + " vs " + mesAnteriorTexto;
        }
        if (diferenciaDescargas < 0) {
            return diferenciaDescargas + " vs " + mesAnteriorTexto;
        }
        return "Sin variación vs " + mesAnteriorTexto;
    }

    private String buildTendenciaTexto(long diferenciaDescargas) {
        if (diferenciaDescargas > 0) {
            return "Activo";
        }
        if (diferenciaDescargas < 0) {
            return "Decreciente";
        }
        return "Estable";
    }

    private String buildTendenciaClase(long diferenciaDescargas) {
        if (diferenciaDescargas > 0) {
            return "text-[#4caf50]";
        }
        if (diferenciaDescargas < 0) {
            return "text-[#ff9800]";
        }
        return "text-on-surface-variant";
    }

    private String buildNovedadesEstadoTexto(LocalDateTime fechaMasAntigua, LocalDateTime now, long novedadesSinLeer) {
        if (fechaMasAntigua == null || novedadesSinLeer == 0) {
            return "Sin novedades";
        }
        long dias = Duration.between(fechaMasAntigua, now).toDays();
        if (dias < 1) {
            return "Reciente ✨";
        }
        if (dias <= 3) {
            return "Revisar pronto ⚠️";
        }
        return "Pendiente desde hace " + dias + " días ⏳";
    }

    private String buildNovedadesEstadoClase(LocalDateTime fechaMasAntigua, LocalDateTime now, long novedadesSinLeer) {
        if (fechaMasAntigua == null || novedadesSinLeer == 0) {
            return "text-on-surface-variant";
        }
        long dias = Duration.between(fechaMasAntigua, now).toDays();
        if (dias < 1) {
            return "text-[#4caf50]";
        }
        if (dias <= 3) {
            return "text-[#ff6f00]";
        }
        return "text-[#ff9800]";
    }

    private String formatAntiguedad(LocalDateTime fecha, LocalDateTime now) {
        Duration duration = Duration.between(fecha, now);
        long days = duration.toDays();
        if (days > 0) {
            return "Desde hace " + days + " día" + (days > 1 ? "s" : "");
        }
        long hours = duration.toHours();
        if (hours > 0) {
            return "Desde hace " + hours + " hora" + (hours > 1 ? "s" : "");
        }
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return "Desde hace " + minutes + " minuto" + (minutes > 1 ? "s" : "");
        }
        return "Desde hace un momento";
    }

    private String formatDecimal(double value) {
        DecimalFormat formatter = new DecimalFormat("0.0");
        return formatter.format(value);
    }

    private String getColorForCategory(String cat) {
        return switch (cat.toUpperCase()) {
            case "EC" -> "#1D9E75";
            case "GR" -> "#378ADD";
            case "EE" -> "#EF9F27";
            case "RE" -> "#E24B4A";
            default -> "#888780";
        };
    }
}

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

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/socio")
public class SocioController {

    private final UsuarioRepository usuarioRepository;
    private final RegistroDescargaRepository registroDescargaRepository;
    private final NotificacionRepository notificacionRepository;
    private final EstudioRepository estudioRepository;
    private final NormativaRepository normativaRepository;
    private final DateUtil dateUtil;

    public SocioController(UsuarioRepository usuarioRepository,
                           RegistroDescargaRepository registroDescargaRepository,
                           NotificacionRepository notificacionRepository,
                           EstudioRepository estudioRepository,
                           NormativaRepository normativaRepository,
                           DateUtil dateUtil) {
        this.usuarioRepository = usuarioRepository;
        this.registroDescargaRepository = registroDescargaRepository;
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

            // Change 1: Mi Actividad Personal
            long totalDescargas = registroDescargaRepository.countByUsuarioIdUsuario(idUsuario);

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

            model.addAttribute("totalDescargas", totalDescargas);
            model.addAttribute("descargasMesActual", descargasMesActual);
            model.addAttribute("diferenciaDescargas", diferenciaDescargas);
            model.addAttribute("novedadesSinLeer", novedadesSinLeer);
            model.addAttribute("ultimaVisita", ultimaVisita);

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

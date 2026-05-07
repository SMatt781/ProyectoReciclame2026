package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.NotificacionItemDto;
import com.example.proyectoreciclame.Dto.SessionUserDto;
import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Entity.SolicitudRegistro;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.Repository.SolicitudRegistroRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.example.proyectoreciclame.Service.AuthenticatedUserService;
import com.example.proyectoreciclame.util.DateUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class AdminNotificacionController {

    private final AuthenticatedUserService authenticatedUserService;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudRegistroRepository solicitudRegistroRepository;
    private final EstudioRepository estudioRepository;
    private final NormativaRepository normativaRepository;
    private final DateUtil dateUtil;

    public AdminNotificacionController(AuthenticatedUserService authenticatedUserService,
                                       UsuarioRepository usuarioRepository,
                                       SolicitudRegistroRepository solicitudRegistroRepository,
                                       EstudioRepository estudioRepository,
                                       NormativaRepository normativaRepository,
                                       DateUtil dateUtil) {
        this.authenticatedUserService = authenticatedUserService;
        this.usuarioRepository = usuarioRepository;
        this.solicitudRegistroRepository = solicitudRegistroRepository;
        this.estudioRepository = estudioRepository;
        this.normativaRepository = normativaRepository;
        this.dateUtil = dateUtil;
    }

    @GetMapping({"/admin/notificaciones", "/socio/notificaciones", "/visualizador/notificaciones"})
    public String verNotificaciones(Model model) {
        SessionUserDto sessionUser = authenticatedUserService.obtenerUsuarioSesion();
        if (sessionUser == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findByCorreoWithRol(sessionUser.getCorreo()).orElse(null);
        if (usuario == null) {
            return "redirect:/login";
        }

        String rol = sessionUser.getRol();
        LocalDateTime referencia = usuario.getUltimoAcceso() != null
                ? usuario.getUltimoAcceso()
                : LocalDateTime.now().minusDays(30);

        List<NotificacionItemDto> notificaciones = new ArrayList<>();

        if (esAdmin(rol)) {
            notificaciones.addAll(construirSolicitudesPendientes());
        }

        notificaciones.addAll(construirNotificacionesDocumentos(rol, referencia));

        if ("SOCIO".equalsIgnoreCase(rol)) {
            construirBienvenida(usuario, referencia).ifPresent(notificaciones::add);
        }

        notificaciones.sort(Comparator.comparing(NotificacionItemDto::getFecha).reversed());

        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("currentSection", esAdmin(rol) ? "admin-notificaciones" : "notificaciones");
        model.addAttribute("currentPage", "notificaciones");

        return "admin/notificaciones";
    }

    private List<NotificacionItemDto> construirSolicitudesPendientes() {
        List<Usuario> pendientes = usuarioRepository.findSolicitudesPendientes(PageRequest.of(0, 8));
        List<NotificacionItemDto> resultado = new ArrayList<>();

        for (Usuario usuario : pendientes) {
            String nombre = construirNombreCompleto(usuario);
            String empresa = usuario.getUsuarioEmpresa() != null
                    ? usuario.getUsuarioEmpresa().getRazonSocial()
                    : "Sin empresa";
            String rolSolicitado = usuario.getRol() != null && usuario.getRol().getNombre() != null
                    ? usuario.getRol().getNombre()
                    : "Sin rol";

            LocalDateTime fecha = usuario.getFechaRegistro();
            String tiempo = dateUtil.formatRelative(fecha);
            String mensaje = String.format(Locale.ROOT,
                    "%s (%s) solicita acceso como %s.", nombre, empresa, rolSolicitado);

            resultado.add(new NotificacionItemDto(
                    "Solicitud de registro",
                    "Solicitud pendiente",
                    mensaje,
                    "SOLICITUD",
                    "/admin/usuarios/solicitudes/" + usuario.getIdUsuario(),
                    tiempo,
                    fecha
            ));
        }

        return resultado;
    }

    private List<NotificacionItemDto> construirNotificacionesDocumentos(String rol, LocalDateTime referencia) {
        List<NotificacionItemDto> resultado = new ArrayList<>();
        boolean esAdmin = esAdmin(rol);

        List<Estudio> estudiosNuevos = estudioRepository.findByFechaCreacionAfter(referencia);
        List<Estudio> estudiosActualizados = estudioRepository
                .findByFechaActualizacionAfterAndFechaCreacionBefore(referencia, referencia);

        for (Estudio estudio : estudiosNuevos) {
            if (!esEstudioPublicable(estudio)) {
                continue;
            }
            resultado.add(buildEstudioNotificacion(estudio, true, rol, esAdmin));
        }

        for (Estudio estudio : estudiosActualizados) {
            if (!esEstudioPublicable(estudio)) {
                continue;
            }
            resultado.add(buildEstudioNotificacion(estudio, false, rol, esAdmin));
        }

        List<Normativa> normativasNuevas = normativaRepository.findByFechaCreacionAfter(referencia);
        List<Normativa> normativasActualizadas = normativaRepository
                .findByFechaActualizacionAfterAndFechaCreacionBefore(referencia, referencia);

        for (Normativa normativa : normativasNuevas) {
            if (!esNormativaPublicable(normativa)) {
                continue;
            }
            resultado.add(buildNormativaNotificacion(normativa, true, esAdmin));
        }

        for (Normativa normativa : normativasActualizadas) {
            if (!esNormativaPublicable(normativa)) {
                continue;
            }
            resultado.add(buildNormativaNotificacion(normativa, false, esAdmin));
        }

        return resultado;
    }

    private Optional<NotificacionItemDto> construirBienvenida(Usuario usuario, LocalDateTime referencia) {
        Optional<SolicitudRegistro> solicitudOpt = buscarSolicitudRelacionada(usuario);
        if (solicitudOpt.isEmpty()) {
            return Optional.empty();
        }

        SolicitudRegistro solicitud = solicitudOpt.get();
        if (!"APROBADO".equalsIgnoreCase(solicitud.getEstado())) {
            return Optional.empty();
        }

        LocalDateTime fecha = solicitud.getFechaResolucion();
        if (fecha == null || fecha.isBefore(referencia)) {
            return Optional.empty();
        }

        String tiempo = dateUtil.formatRelative(fecha);

        return Optional.of(new NotificacionItemDto(
                "Bienvenido a Reciclame",
                "Acceso aprobado",
                "Tu acceso ha sido aprobado. Ya puedes explorar los documentos disponibles.",
                "BIENVENIDA",
                "/socio",
                tiempo,
                fecha
        ));
    }

    private NotificacionItemDto buildEstudioNotificacion(Estudio estudio, boolean esNuevo, String rol, boolean esAdmin) {
        String acceso = resolveAccesoEstudio(estudio.getTipoAcceso(), rol);
        String etiqueta = esNuevo ? "Documento publicado" : "Documento actualizado";
        String titulo = esNuevo ? "Nuevo estudio publicado" : "Estudio actualizado";
        String mensaje = String.format(Locale.ROOT,
                "Estudio \"%s\" (%s).", estudio.getTitulo(), acceso);
        LocalDateTime fecha = esNuevo ? estudio.getFechaCreacion() : estudio.getFechaActualizacion();

        String enlace = esAdmin
                ? "/admin/estudios/" + estudio.getIdEstudio()
                : "/estudios/" + estudio.getIdEstudio();

        return new NotificacionItemDto(
                titulo,
                etiqueta,
                mensaje,
                esNuevo ? "ESTUDIO_NUEVO" : "ESTUDIO_ACTUALIZADO",
                enlace,
                dateUtil.formatRelative(fecha),
                fecha
        );
    }

    private NotificacionItemDto buildNormativaNotificacion(Normativa normativa, boolean esNuevo, boolean esAdmin) {
        String acceso = resolveAccesoNormativa(normativa.getAcceso());
        String etiqueta = esNuevo ? "Documento publicado" : "Documento actualizado";
        String titulo = esNuevo ? "Nueva normativa publicada" : "Normativa actualizada";
        String mensaje = String.format(Locale.ROOT,
                "Normativa \"%s\" (%s).", normativa.getTitulo(), acceso);
        LocalDateTime fecha = esNuevo ? normativa.getFechaCreacion() : normativa.getFechaActualizacion();

        String enlace = esAdmin
                ? "/admin/normativas/" + normativa.getIdNormativa()
                : "/normativas/" + normativa.getIdNormativa();

        return new NotificacionItemDto(
                titulo,
                etiqueta,
                mensaje,
                esNuevo ? "NORMATIVA_NUEVA" : "NORMATIVA_ACTUALIZADA",
                enlace,
                dateUtil.formatRelative(fecha),
                fecha
        );
    }

    private boolean esAdmin(String rol) {
        return "ADMIN".equalsIgnoreCase(rol) || "SUPERADMIN".equalsIgnoreCase(rol);
    }

    private boolean esEstudioPublicable(Estudio estudio) {
        return estudio != null && estudio.getEstado() == Estudio.EstadoEstudio.VIGENTE;
    }

    private boolean esNormativaPublicable(Normativa normativa) {
        if (normativa == null || normativa.getEstado() == null) {
            return false;
        }
        return normativa.getEstado() == Normativa.EstadoNormativa.PUBLICADA
                || normativa.getEstado() == Normativa.EstadoNormativa.VIGENTE;
    }

    private String resolveAccesoEstudio(Estudio.TipoAcceso tipoAcceso, String rol) {
        if (tipoAcceso == null) {
            return "Solo lectura";
        }
        if ("VISUALIZADOR".equalsIgnoreCase(rol) && tipoAcceso == Estudio.TipoAcceso.DESCARGA) {
            return "Solo lectura";
        }
        return tipoAcceso == Estudio.TipoAcceso.DESCARGA ? "Descargable" : "Solo lectura";
    }

    private String resolveAccesoNormativa(Normativa.AccesoNormativa accesoNormativa) {
        if (accesoNormativa == null) {
            return "Solo lectura";
        }
        return accesoNormativa == Normativa.AccesoNormativa.GRATIS ? "Descargable" : "Solo lectura";
    }

    private Optional<SolicitudRegistro> buscarSolicitudRelacionada(Usuario usuario) {
        Optional<SolicitudRegistro> porDni = solicitudRegistroRepository
                .findTopByDniOrderByFechaSolicitudDesc(usuario.getDni());

        if (porDni.isPresent()) {
            return porDni;
        }

        return solicitudRegistroRepository.findTopByCorreoOrderByFechaSolicitudDesc(usuario.getCorreo());
    }

    private String construirNombreCompleto(Usuario usuario) {
        StringBuilder sb = new StringBuilder();
        if (usuario.getNombres() != null && !usuario.getNombres().isBlank()) {
            sb.append(usuario.getNombres());
        }
        if (usuario.getApellidoPaterno() != null && !usuario.getApellidoPaterno().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(usuario.getApellidoPaterno());
        }
        if (usuario.getApellidoMaterno() != null && !usuario.getApellidoMaterno().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(usuario.getApellidoMaterno());
        }
        return sb.toString().trim();
    }
}
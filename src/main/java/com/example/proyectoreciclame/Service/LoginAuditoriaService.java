package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Entity.IntentoLogin;
import com.example.proyectoreciclame.Entity.RegistroSesion;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.IntentoLoginRepository;
import com.example.proyectoreciclame.Repository.RegistroSesionRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LoginAuditoriaService {

    private final UsuarioRepository usuarioRepository;
    private final IntentoLoginRepository intentoLoginRepository;
    private final RegistroSesionRepository registroSesionRepository;

    public LoginAuditoriaService(UsuarioRepository usuarioRepository,
                                 IntentoLoginRepository intentoLoginRepository,
                                 RegistroSesionRepository registroSesionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.intentoLoginRepository = intentoLoginRepository;
        this.registroSesionRepository = registroSesionRepository;
    }

    @Transactional
    public void registrarIntentoExitoso(String correo, HttpServletRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoWithRol(correo).orElse(null);

        IntentoLogin intento = new IntentoLogin();
        intento.setUsuario(usuario);
        intento.setCorreo(correo);
        intento.setFecha(LocalDateTime.now());
        intento.setIp(obtenerIp(request));
        intento.setExitoso(true);
        intentoLoginRepository.save(intento);

        if (usuario != null) {
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);

            RegistroSesion sesion = new RegistroSesion();
            sesion.setUsuario(usuario);
            sesion.setToken(UUID.randomUUID().toString());
            sesion.setFechaInicio(LocalDateTime.now());
            sesion.setEstado("VIGENTE");
            sesion.setIp(obtenerIp(request));
            sesion.setAgenteUsuario(request.getHeader("User-Agent"));
            registroSesionRepository.save(sesion);

            HttpSession httpSession = request.getSession();
            httpSession.setAttribute("ID_SESION_BD", sesion.getIdSesion());
        }
    }

    @Transactional
    public void registrarIntentoFallido(String correo, HttpServletRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoWithRol(correo).orElse(null);

        IntentoLogin intento = new IntentoLogin();
        intento.setUsuario(usuario);
        intento.setCorreo(correo);
        intento.setFecha(LocalDateTime.now());
        intento.setIp(obtenerIp(request));
        intento.setExitoso(false);
        intentoLoginRepository.save(intento);
    }

    @Transactional
    public void cerrarSesion(HttpServletRequest request, String correo) {
        Object idSesionObj = request.getSession(false) != null
                ? request.getSession(false).getAttribute("ID_SESION_BD")
                : null;

        if (idSesionObj instanceof Long idSesion) {
            RegistroSesion sesion = registroSesionRepository.findById(idSesion).orElse(null);
            if (sesion != null) {
                LocalDateTime fin = LocalDateTime.now();
                sesion.setFechaFin(fin);
                sesion.setEstado("FINALIZADA");

                if (sesion.getFechaInicio() != null) {
                    long minutos = Duration.between(sesion.getFechaInicio(), fin).toMinutes();
                    sesion.setDuracionMinutos((int) Math.max(minutos, 0));
                }

                registroSesionRepository.save(sesion);
                return;
            }
        }

        if (correo != null) {
            Usuario usuario = usuarioRepository.findByCorreoWithRol(correo).orElse(null);
            if (usuario != null) {
                RegistroSesion sesion = registroSesionRepository
                        .findTopByUsuarioAndEstadoOrderByFechaInicioDesc(usuario, "VIGENTE")
                        .orElse(null);

                if (sesion != null) {
                    LocalDateTime fin = LocalDateTime.now();
                    sesion.setFechaFin(fin);
                    sesion.setEstado("FINALIZADA");

                    if (sesion.getFechaInicio() != null) {
                        long minutos = Duration.between(sesion.getFechaInicio(), fin).toMinutes();
                        sesion.setDuracionMinutos((int) Math.max(minutos, 0));
                    }

                    registroSesionRepository.save(sesion);
                }
            }
        }
    }

    private String obtenerIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
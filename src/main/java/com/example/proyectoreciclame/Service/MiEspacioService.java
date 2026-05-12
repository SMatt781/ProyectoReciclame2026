package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Entity.ContenidoGuardado;
import com.example.proyectoreciclame.Entity.EspacioCarpeta;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.ContenidoGuardadoRepository;
import com.example.proyectoreciclame.Repository.EspacioCarpetaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MiEspacioService {

    private final ContenidoGuardadoRepository contenidoGuardadoRepository;
    private final EspacioCarpetaRepository espacioCarpetaRepository;
    private final UsuarioRepository usuarioRepository;

    public MiEspacioService(ContenidoGuardadoRepository contenidoGuardadoRepository,
                            EspacioCarpetaRepository espacioCarpetaRepository,
                            UsuarioRepository usuarioRepository) {
        this.contenidoGuardadoRepository = contenidoGuardadoRepository;
        this.espacioCarpetaRepository = espacioCarpetaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ── Contenido guardado ────────────────────────────────────────────────────

    public List<ContenidoGuardado> listarPorUsuario(Long idUsuario) {
        return contenidoGuardadoRepository.findByUsuario_IdUsuarioOrderByFechaGuardadoDesc(idUsuario);
    }

    public List<ContenidoGuardado> listarPorCarpeta(Long idUsuario, Long idCarpeta) {
        return contenidoGuardadoRepository
                .findByUsuario_IdUsuarioAndCarpeta_IdCarpetaOrderByFechaGuardadoDesc(idUsuario, idCarpeta);
    }

    public List<ContenidoGuardado> listarSinCarpeta(Long idUsuario) {
        return contenidoGuardadoRepository
                .findByUsuario_IdUsuarioAndCarpetaIsNullOrderByFechaGuardadoDesc(idUsuario);
    }

    public boolean estaGuardado(Long idUsuario, String tipoDocumento, Long idDocumento) {
        return contenidoGuardadoRepository.existsByUsuario_IdUsuarioAndTipoDocumentoAndIdDocumento(
                idUsuario, tipoDocumento, idDocumento);
    }

    @Transactional
    public void guardar(Long idUsuario, String tipoDocumento, Long idDocumento, String nombreDocumento) {
        boolean existe = contenidoGuardadoRepository.existsByUsuario_IdUsuarioAndTipoDocumentoAndIdDocumento(
                idUsuario, tipoDocumento, idDocumento);
        if (existe) return;

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
        if (usuarioOpt.isEmpty()) return;

        ContenidoGuardado guardado = new ContenidoGuardado();
        guardado.setUsuario(usuarioOpt.get());
        guardado.setTipoDocumento(tipoDocumento);
        guardado.setIdDocumento(idDocumento);
        guardado.setNombreDocumento(nombreDocumento);
        guardado.setFechaGuardado(LocalDateTime.now());
        contenidoGuardadoRepository.save(guardado);
    }

    @Transactional
    public void eliminar(Long idGuardado, Long idUsuario) {
        contenidoGuardadoRepository.findById(idGuardado).ifPresent(guardado -> {
            if (guardado.getUsuario().getIdUsuario().equals(idUsuario)) {
                contenidoGuardadoRepository.delete(guardado);
            }
        });
    }

    @Transactional
    public void moverACarpeta(Long idGuardado, Long idUsuario, Long idCarpeta) {
        contenidoGuardadoRepository.findById(idGuardado).ifPresent(guardado -> {
            if (!guardado.getUsuario().getIdUsuario().equals(idUsuario)) return;

            if (idCarpeta == null) {
                guardado.setCarpeta(null);
            } else {
                espacioCarpetaRepository.findByIdCarpetaAndUsuario_IdUsuario(idCarpeta, idUsuario)
                        .ifPresent(guardado::setCarpeta);
            }
            contenidoGuardadoRepository.save(guardado);
        });
    }

    public long contarPorUsuario(Long idUsuario) {
        return contenidoGuardadoRepository.countByUsuario_IdUsuario(idUsuario);
    }

    public long contarPorTipo(Long idUsuario, String tipoDocumento) {
        return contenidoGuardadoRepository.countByUsuario_IdUsuarioAndTipoDocumento(idUsuario, tipoDocumento);
    }

    // ── Carpetas ──────────────────────────────────────────────────────────────

    public List<EspacioCarpeta> listarCarpetas(Long idUsuario) {
        return espacioCarpetaRepository.findByUsuario_IdUsuarioOrderByNombreAsc(idUsuario);
    }

    public Map<Long, Long> conteosPorCarpeta(Long idUsuario) {
        Map<Long, Long> map = new HashMap<>();
        espacioCarpetaRepository.countPorCarpeta(idUsuario)
                .forEach(row -> map.put((Long) row[0], (Long) row[1]));
        return map;
    }

    @Transactional
    public boolean crearCarpeta(Long idUsuario, String nombre, String color, String emoji) {
        if (espacioCarpetaRepository.existsByUsuario_IdUsuarioAndNombre(idUsuario, nombre.trim())) {
            return false;
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
        if (usuarioOpt.isEmpty()) return false;

        EspacioCarpeta carpeta = new EspacioCarpeta();
        carpeta.setUsuario(usuarioOpt.get());
        carpeta.setNombre(nombre.trim());
        carpeta.setColor(color != null && !color.isBlank() ? color : "#006d37");
        carpeta.setEmoji(emoji != null && !emoji.isBlank() ? emoji.trim() : null);
        espacioCarpetaRepository.save(carpeta);
        return true;
    }

    @Transactional
    public boolean editarCarpeta(Long idUsuario, Long idCarpeta, String nombre, String color, String emoji) {
        Optional<EspacioCarpeta> opt = espacioCarpetaRepository
                .findByIdCarpetaAndUsuario_IdUsuario(idCarpeta, idUsuario);
        if (opt.isEmpty()) return false;

        EspacioCarpeta carpeta = opt.get();
        String nuevoNombre = nombre.trim();

        if (!carpeta.getNombre().equals(nuevoNombre) &&
                espacioCarpetaRepository.existsByUsuario_IdUsuarioAndNombre(idUsuario, nuevoNombre)) {
            return false;
        }

        carpeta.setNombre(nuevoNombre);
        carpeta.setColor(color != null && !color.isBlank() ? color : "#006d37");
        carpeta.setEmoji(emoji != null && !emoji.isBlank() ? emoji.trim() : null);
        espacioCarpetaRepository.save(carpeta);
        return true;
    }

    @Transactional
    public void eliminarCarpeta(Long idUsuario, Long idCarpeta) {
        espacioCarpetaRepository.findByIdCarpetaAndUsuario_IdUsuario(idCarpeta, idUsuario)
                .ifPresent(carpeta -> {
                    // Los documentos quedan con carpeta=NULL (SET NULL en FK)
                    espacioCarpetaRepository.delete(carpeta);
                });
    }
}

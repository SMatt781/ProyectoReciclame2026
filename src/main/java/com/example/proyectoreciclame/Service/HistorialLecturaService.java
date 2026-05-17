package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Entity.HistorialLectura;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.HistorialLecturaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class HistorialLecturaService {

    private final HistorialLecturaRepository historialLecturaRepository;
    private final UsuarioRepository usuarioRepository;

    public HistorialLecturaService(HistorialLecturaRepository historialLecturaRepository,
                                   UsuarioRepository usuarioRepository) {
        this.historialLecturaRepository = historialLecturaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrarOActualizar(Long idUsuario, String tipoDocumento, Long idDocumento, String nombre) {
        Optional<HistorialLectura> existente = historialLecturaRepository
                .findByUsuario_IdUsuarioAndTipoDocumentoAndIdDocumento(idUsuario, tipoDocumento, idDocumento);

        if (existente.isPresent()) {
            HistorialLectura lectura = existente.get();
            lectura.setVecesVisto(lectura.getVecesVisto() + 1);
            lectura.setUltimaLectura(LocalDateTime.now());
            lectura.setNombreDocumento(nombre);
            historialLecturaRepository.save(lectura);
        } else {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
            if (usuarioOpt.isEmpty()) return;

            HistorialLectura nueva = new HistorialLectura();
            nueva.setUsuario(usuarioOpt.get());
            nueva.setTipoDocumento(tipoDocumento);
            nueva.setIdDocumento(idDocumento);
            nueva.setNombreDocumento(nombre);
            nueva.setVecesVisto(1);
            nueva.setPrimeraLectura(LocalDateTime.now());
            nueva.setUltimaLectura(LocalDateTime.now());
            historialLecturaRepository.save(nueva);
        }
    }

    public List<HistorialLectura> listarRecientes(Long idUsuario, int limite) {
        return historialLecturaRepository.findByUsuario_IdUsuarioOrderByUltimaLecturaDesc(
                idUsuario, PageRequest.of(0, limite));
    }

    public long contarPorUsuario(Long idUsuario) {
        return historialLecturaRepository.countByUsuario_IdUsuario(idUsuario);
    }
}

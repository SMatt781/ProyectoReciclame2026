package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.HistorialLectura;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistorialLecturaRepository extends JpaRepository<HistorialLectura, Long> {

    List<HistorialLectura> findByUsuario_IdUsuarioOrderByUltimaLecturaDesc(Long idUsuario, Pageable pageable);

    Optional<HistorialLectura> findByUsuario_IdUsuarioAndTipoDocumentoAndIdDocumento(
            Long idUsuario, String tipoDocumento, Long idDocumento);

    long countByUsuario_IdUsuario(Long idUsuario);
}

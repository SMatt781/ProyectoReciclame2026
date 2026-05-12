package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.ContenidoGuardado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContenidoGuardadoRepository extends JpaRepository<ContenidoGuardado, Long> {

    List<ContenidoGuardado> findByUsuario_IdUsuarioOrderByFechaGuardadoDesc(Long idUsuario);

    Optional<ContenidoGuardado> findByUsuario_IdUsuarioAndTipoDocumentoAndIdDocumento(
            Long idUsuario, String tipoDocumento, Long idDocumento);

    boolean existsByUsuario_IdUsuarioAndTipoDocumentoAndIdDocumento(
            Long idUsuario, String tipoDocumento, Long idDocumento);

    long countByUsuario_IdUsuario(Long idUsuario);

    List<ContenidoGuardado> findByUsuario_IdUsuarioAndCarpeta_IdCarpetaOrderByFechaGuardadoDesc(
            Long idUsuario, Long idCarpeta);

    List<ContenidoGuardado> findByUsuario_IdUsuarioAndCarpetaIsNullOrderByFechaGuardadoDesc(
            Long idUsuario);

    long countByUsuario_IdUsuarioAndTipoDocumento(Long idUsuario, String tipoDocumento);
}

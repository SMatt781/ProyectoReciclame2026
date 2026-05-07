package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.RegistroDescarga;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistroDescargaRepository extends JpaRepository<RegistroDescarga, Long> {

    @EntityGraph(attributePaths = {"usuario"})
    @Query(value = """
            SELECT rd
            FROM RegistroDescarga rd
            JOIN rd.usuario u
            WHERE u.eliminadoEn IS NULL
              AND (:texto IS NULL OR
                   LOWER(u.nombres) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(u.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(COALESCE(u.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(rd.nombreDocumento) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:tipo IS NULL OR UPPER(rd.tipoDocumento) = UPPER(:tipo))
              AND (:fechaInicio IS NULL OR rd.fechaDescarga >= :fechaInicio)
              AND (:fechaFin IS NULL OR rd.fechaDescarga <= :fechaFin)
            ORDER BY rd.fechaDescarga DESC
            """,
            countQuery = """
            SELECT COUNT(rd)
            FROM RegistroDescarga rd
            JOIN rd.usuario u
            WHERE u.eliminadoEn IS NULL
              AND (:texto IS NULL OR
                   LOWER(u.nombres) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(u.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(COALESCE(u.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(rd.nombreDocumento) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:tipo IS NULL OR UPPER(rd.tipoDocumento) = UPPER(:tipo))
              AND (:fechaInicio IS NULL OR rd.fechaDescarga >= :fechaInicio)
              AND (:fechaFin IS NULL OR rd.fechaDescarga <= :fechaFin)
            """)
    Page<RegistroDescarga> buscarFiltrado(@Param("texto") String texto,
                                          @Param("tipo") String tipo,
                                          @Param("fechaInicio") LocalDateTime fechaInicio,
                                          @Param("fechaFin") LocalDateTime fechaFin,
                                          Pageable pageable);

    long countByTipoDocumento(String tipoDocumento);

    long countByIdDocumentoAndTipoDocumento(Long idDocumento, String tipoDocumento);

    long countByUsuarioIdUsuario(Long idUsuario);

    @Query("SELECT COUNT(rd) FROM RegistroDescarga rd WHERE rd.usuario.idUsuario = :idUsuario AND UPPER(rd.tipoDocumento) = UPPER(:tipoDocumento)")
    long countByUsuarioIdUsuarioAndTipoDocumento(@Param("idUsuario") Long idUsuario, @Param("tipoDocumento") String tipoDocumento);

    @Query("SELECT COUNT(rd) FROM RegistroDescarga rd WHERE rd.usuario.idUsuario = :idUsuario AND rd.fechaDescarga >= :fechaInicio AND rd.fechaDescarga <= :fechaFin")
    long countByUsuarioIdUsuarioAndFechaDescargaBetween(@Param("idUsuario") Long idUsuario, @Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT rd FROM RegistroDescarga rd WHERE rd.usuario.idUsuario = :idUsuario ORDER BY rd.fechaDescarga DESC LIMIT 3")
    List<RegistroDescarga> findTop3ByUsuarioIdUsuarioOrderByFechaDescargaDesc(@Param("idUsuario") Long idUsuario);

    // Top 5 documentos más descargados
    @Query(value = "SELECT nombre_documento, tipo_documento, COUNT(*) AS total FROM registro_descarga GROUP BY nombre_documento, tipo_documento ORDER BY total DESC LIMIT 5", nativeQuery = true)
    List<Object[]> topDocumentosMasDescargados();

    // En RegistroDescargaRepository agrega:
    @Query(value = """
    SELECT COUNT(*) FROM registro_descarga rd
    JOIN usuarios u ON rd.id_usuario = u.id_usuario
    JOIN rol r ON u.id_rol = r.id_rol
    WHERE r.nombre = :rol
    """, nativeQuery = true)
    Long countByRolUsuario(@Param("rol") String rol);
}
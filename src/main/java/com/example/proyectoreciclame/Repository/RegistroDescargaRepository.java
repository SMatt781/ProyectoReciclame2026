package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.RegistroDescarga;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

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
}
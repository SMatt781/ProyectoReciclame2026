package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.RegistroSesion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RegistroSesionRepository extends JpaRepository<RegistroSesion, Long> {

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    @Query(value = """
            SELECT rs
            FROM RegistroSesion rs
            JOIN rs.usuario u
            JOIN u.rol r
            WHERE u.eliminadoEn IS NULL
              AND (:texto IS NULL OR
                   LOWER(u.nombres) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(u.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(COALESCE(u.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:fechaInicio IS NULL OR rs.fechaInicio >= :fechaInicio)
              AND (:fechaFin IS NULL OR rs.fechaInicio <= :fechaFin)
              AND (:rol IS NULL OR UPPER(r.nombre) = UPPER(:rol))
            ORDER BY rs.fechaInicio DESC
            """,
            countQuery = """
            SELECT COUNT(rs)
            FROM RegistroSesion rs
            JOIN rs.usuario u
            JOIN u.rol r
            WHERE u.eliminadoEn IS NULL
              AND (:texto IS NULL OR
                   LOWER(u.nombres) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(u.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(COALESCE(u.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:fechaInicio IS NULL OR rs.fechaInicio >= :fechaInicio)
              AND (:fechaFin IS NULL OR rs.fechaInicio <= :fechaFin)
              AND (:rol IS NULL OR UPPER(r.nombre) = UPPER(:rol))
            """)
    Page<RegistroSesion> buscarFiltrado(@Param("texto") String texto,
                                        @Param("fechaInicio") LocalDateTime fechaInicio,
                                        @Param("fechaFin") LocalDateTime fechaFin,
                                        @Param("rol") String rol,
                                        Pageable pageable);
}
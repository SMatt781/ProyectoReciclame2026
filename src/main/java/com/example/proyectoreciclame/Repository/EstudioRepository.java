package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Dto.EstudioDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EstudioRepository extends JpaRepository<Estudio, Long> {

       @Query("SELECT new com.example.proyectoreciclame.Dto.EstudioDTO(e.idEstudio, e.titulo, e.descripcion, e.anio, CAST(e.formato AS string), CAST(e.estado AS string), CAST(e.tipoAcceso AS string), e.fechaPublicacion) FROM Estudio e")
       List<EstudioDTO> findAllEstudioDTO();

       @Query("SELECT new com.example.proyectoreciclame.Dto.EstudioDTO(e.idEstudio, e.titulo, e.descripcion, e.anio, CAST(e.formato AS string), CAST(e.estado AS string), CAST(e.tipoAcceso AS string), e.fechaPublicacion) FROM Estudio e WHERE LOWER(e.titulo) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       List<EstudioDTO> findByTituloContainingIgnoreCaseDTO(@Param("keyword") String keyword);

       @Query("SELECT new com.example.proyectoreciclame.Dto.EstudioDTO(e.idEstudio, e.titulo, e.descripcion, e.anio, CAST(e.formato AS string), CAST(e.estado AS string), CAST(e.tipoAcceso AS string), e.fechaPublicacion) FROM Estudio e "
                     +
                     "WHERE (:anio IS NULL OR e.anio = :anio) " +
                     "AND (cast(:fechaInicio as date) IS NULL OR e.fechaPublicacion >= :fechaInicio) " +
                     "AND (cast(:fechaFin as date) IS NULL OR e.fechaPublicacion <= :fechaFin) " +
                     "AND (:hasFormatos = false OR CAST(e.formato AS string) IN :formatos) " +
                     "AND (:hasEstados = false OR CAST(e.estado AS string) IN :estados)")
       List<EstudioDTO> findWithAdvancedFiltersDTO(@Param("anio") Integer anio,
                     @Param("fechaInicio") LocalDate fechaInicio,
                     @Param("fechaFin") LocalDate fechaFin,
                     @Param("hasFormatos") boolean hasFormatos,
                     @Param("formatos") List<String> formatos,
                     @Param("hasEstados") boolean hasEstados,
                     @Param("estados") List<String> estados);

       List<Estudio> findByFechaCreacionAfterAndEliminadoEnIsNull(LocalDateTime fechaCreacion);
       List<Estudio> findByFechaActualizacionAfterAndFechaCreacionBeforeAndEliminadoEnIsNull(LocalDateTime fechaActualizacion, LocalDateTime fechaCreacion);


    @Query("""
    SELECT e
    FROM Estudio e
    WHERE e.eliminadoEn IS NULL
      AND (:search IS NULL OR LOWER(e.titulo) LIKE LOWER(CONCAT('%', :search, '%')))
      AND (:anio IS NULL OR e.anio = :anio)
      AND (:fechaInicio IS NULL OR e.fechaPublicacion >= :fechaInicio)
      AND (:fechaFin IS NULL OR e.fechaPublicacion <= :fechaFin)
      AND (:hasFormatos = false OR CAST(e.formato AS string) IN :formatos)
      AND (:hasEstados = false OR CAST(e.estado AS string) IN :estados)
    ORDER BY e.fechaCreacion DESC
""")
    List<Estudio> filtrarAdmin(
            @Param("search") String search,
            @Param("anio") Integer anio,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("hasFormatos") boolean hasFormatos,
            @Param("formatos") List<String> formatos,
            @Param("hasEstados") boolean hasEstados,
            @Param("estados") List<String> estados
    );

    long countByEstadoAndEliminadoEnIsNull(Estudio.EstadoEstudio estado);

    long countByEstadoAndFormatoAndEliminadoEnIsNull(
            Estudio.EstadoEstudio estado,
            Estudio.FormatoEstudio formato
    );

    List<Estudio> findTop3ByEliminadoEnIsNullOrderByFechaActualizacionDesc();

    long countByEliminadoEnIsNull();

    long countByFechaPublicacionAfterAndEliminadoEnIsNull(LocalDate fechaPublicacion);

    /** Estudios que comparten al menos una categoría. JPQL sin Pageable para evitar HHH90003004. */
    @Query("SELECT DISTINCT e FROM Estudio e JOIN e.categorias c " +
           "WHERE e.idEstudio <> :id AND e.eliminadoEn IS NULL " +
           "AND c IN (SELECT c2 FROM Estudio e2 JOIN e2.categorias c2 WHERE e2.idEstudio = :id) " +
           "ORDER BY e.fechaPublicacion DESC")
    List<Estudio> findSimilaresPorCategorias(@Param("id") Long id);

    /** Estudios del mismo año como fallback. */
    @Query("SELECT e FROM Estudio e WHERE e.anio = :anio AND e.idEstudio <> :id " +
           "AND e.eliminadoEn IS NULL ORDER BY e.fechaPublicacion DESC")
    List<Estudio> findSimilaresPorAnio(@Param("id") Long id, @Param("anio") Integer anio);

    @Query("""
            SELECT FUNCTION('year', e.fechaPublicacion), COUNT(e)
            FROM Estudio e
            WHERE e.eliminadoEn IS NULL
              AND e.fechaPublicacion IS NOT NULL
            GROUP BY FUNCTION('year', e.fechaPublicacion)
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> findActiveYearsByPublicacion();

}

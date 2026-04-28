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

       List<Estudio> findByFechaCreacionAfter(LocalDateTime fechaCreacion);
       List<Estudio> findByFechaActualizacionAfterAndFechaCreacionBefore(LocalDateTime fechaActualizacion, LocalDateTime fechaCreacion);
}
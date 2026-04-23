package com.example.reciclameproyecto.repository;

import com.example.reciclameproyecto.entity.Normativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NormativaRepository extends JpaRepository<Normativa, Long> {

    @Query("SELECT DISTINCT n FROM Normativa n LEFT JOIN FETCH n.categorias")
    List<Normativa> findAllNormativas();

    @Query("SELECT DISTINCT n FROM Normativa n LEFT JOIN FETCH n.categorias " +
           "WHERE LOWER(n.titulo) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(n.codigo) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(n.organismoEmisor) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Normativa> findByKeyword(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT n FROM Normativa n LEFT JOIN FETCH n.categorias " +
           "WHERE (:hasAnio = false OR n.anio = :anio) " +
           "AND (cast(:fechaInicio as timestamp) IS NULL OR n.fechaCreacion >= :fechaInicio) " +
           "AND (cast(:fechaFin as timestamp) IS NULL OR n.fechaCreacion <= :fechaFin) " +
           "AND (:hasEstado = false OR CAST(n.estado AS string) IN :estados) " +
           "AND (:hasAcceso = false OR CAST(n.acceso AS string) IN :accesos) " +
           "AND (:hasObligatoriedad = false OR n.obligatoriedad IN :obligatoriedades) " +
           "AND (:hasCategoria = false OR EXISTS (SELECT 1 FROM n.categorias c WHERE c.nombre IN :categorias))")
    List<Normativa> findWithAdvancedFilters(
            @Param("hasAnio") boolean hasAnio, @Param("anio") Integer anio,
            @Param("fechaInicio") java.time.LocalDateTime fechaInicio,
            @Param("fechaFin") java.time.LocalDateTime fechaFin,
            @Param("hasEstado") boolean hasEstado, @Param("estados") List<String> estados,
            @Param("hasAcceso") boolean hasAcceso, @Param("accesos") List<String> accesos,
            @Param("hasObligatoriedad") boolean hasObligatoriedad, @Param("obligatoriedades") List<String> obligatoriedades,
            @Param("hasCategoria") boolean hasCategoria, @Param("categorias") List<String> categorias
    );
}

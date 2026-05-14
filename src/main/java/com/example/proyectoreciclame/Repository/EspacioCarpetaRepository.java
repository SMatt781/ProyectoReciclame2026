package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.EspacioCarpeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EspacioCarpetaRepository extends JpaRepository<EspacioCarpeta, Long> {

    List<EspacioCarpeta> findByUsuario_IdUsuarioOrderByNombreAsc(Long idUsuario);

    Optional<EspacioCarpeta> findByIdCarpetaAndUsuario_IdUsuario(Long idCarpeta, Long idUsuario);

    boolean existsByUsuario_IdUsuarioAndNombre(Long idUsuario, String nombre);

    @Query("SELECT c.carpeta.idCarpeta, COUNT(c) FROM ContenidoGuardado c " +
           "WHERE c.usuario.idUsuario = :idUsuario AND c.carpeta IS NOT NULL " +
           "GROUP BY c.carpeta.idCarpeta")
    List<Object[]> countPorCarpeta(@Param("idUsuario") Long idUsuario);
}

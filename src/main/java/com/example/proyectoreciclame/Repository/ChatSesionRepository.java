package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.ChatSesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSesionRepository extends JpaRepository<ChatSesion, Long> {

    /** Busca la sesión activa del usuario, si existe. */
    @Query("SELECT s FROM ChatSesion s WHERE s.usuario.idUsuario = :idUsuario AND s.activa = true ORDER BY s.fechaInicio DESC")
    Optional<ChatSesion> findSesionActivaByUsuario(@Param("idUsuario") Long idUsuario);

    /** Cuenta sesiones totales de un usuario. */
    long countByUsuario_IdUsuario(Long idUsuario);

    /** Cuenta sesiones activas en toda la plataforma. */
    long countByActivaTrue();
}

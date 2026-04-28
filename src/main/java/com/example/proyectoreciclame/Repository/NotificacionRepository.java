package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.Notificacion;
import com.example.proyectoreciclame.Entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    long countByUsuarioAndLeidoFalse(Usuario usuario);

    @Query("SELECT COUNT(n) FROM Notificacion n WHERE n.usuario.idUsuario = :idUsuario AND n.leido = false")
    long countUnreadNovedadesByUsuario(@Param("idUsuario") Long idUsuario);
}
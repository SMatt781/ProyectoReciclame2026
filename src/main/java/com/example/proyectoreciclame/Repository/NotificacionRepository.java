package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.Notificacion;
import com.example.proyectoreciclame.Entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    long countByUsuarioAndLeidoFalse(Usuario usuario);
}
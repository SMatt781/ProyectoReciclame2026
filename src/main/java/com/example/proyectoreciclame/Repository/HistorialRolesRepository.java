package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.HistorialRoles;
import com.example.proyectoreciclame.Entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialRolesRepository extends JpaRepository<HistorialRoles, Long> {

    // Trae todos los cambios de un usuario ordenados del más reciente al más antiguo
    List<HistorialRoles> findByUsuarioAfectadoOrderByFechaCambioDesc(Usuario usuario);
}

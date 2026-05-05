package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombreIgnoreCase(String nombre);
}
package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.SolicitudRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolicitudRegistroRepository extends JpaRepository<SolicitudRegistro, Long> {

    Optional<SolicitudRegistro> findTopByDniOrderByFechaSolicitudDesc(String dni);

    Optional<SolicitudRegistro> findTopByCorreoOrderByFechaSolicitudDesc(String correo);

    long countByEstado(String estado);
}
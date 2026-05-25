package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.Identificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentificacionRepository extends JpaRepository<Identificacion, Long> {

    boolean existsByNumero(String numero);

    Optional<Identificacion> findByUsuario_IdUsuario(Long idUsuario);
}

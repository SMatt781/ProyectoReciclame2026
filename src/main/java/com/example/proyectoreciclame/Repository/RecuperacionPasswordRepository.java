package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.RecuperacionPassword;
import com.example.proyectoreciclame.Entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RecuperacionPasswordRepository extends JpaRepository<RecuperacionPassword, Long> {

    Optional<RecuperacionPassword> findTopByUsuarioAndUsadoFalseOrderByCreadoEnDesc(Usuario usuario);

    Optional<RecuperacionPassword> findTopByUsuarioAndCodigoAndUsadoFalseOrderByCreadoEnDesc(
            Usuario usuario, String codigo
    );

    void deleteByUsuario(Usuario usuario);

    long countByUsuarioAndUsadoFalseAndFechaExpiracionAfter(
            Usuario usuario, LocalDateTime fecha
    );
    long countByUsadoFalse();
}
package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.IntentoLogin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, Long> {
    long countByFechaAfterAndExitosoFalse(LocalDateTime fecha);
}
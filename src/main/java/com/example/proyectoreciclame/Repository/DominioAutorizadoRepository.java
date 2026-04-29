package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.DominioAutorizado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DominioAutorizadoRepository extends JpaRepository<DominioAutorizado, Integer> {
    Page<DominioAutorizado> findByNombreDominioContainingIgnoreCase(String texto, Pageable pageable);
}

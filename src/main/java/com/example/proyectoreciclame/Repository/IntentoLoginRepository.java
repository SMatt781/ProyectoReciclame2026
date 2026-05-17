package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.IntentoLogin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, Long> {
    long countByFechaAfterAndExitosoFalse(LocalDateTime fecha);
    long countByCorreoAndExitosoFalseAndFechaAfter(String correo, LocalDateTime fecha);

    // Elimina los intentos fallidos recientes de un correo
// Se llama al desbloquear manualmente para resetear el contador
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM intento_login WHERE correo = :correo AND exitoso = false AND fecha >= :desde",
            nativeQuery = true)
    void eliminarIntentosFallidosRecientes(@Param("correo") String correo,
                                           @Param("desde") LocalDateTime desde);
}
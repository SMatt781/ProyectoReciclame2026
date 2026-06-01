package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    // Cuenta cuántas consultas hizo el usuario hoy de un tipo dado
    @Query("SELECT COUNT(l) FROM AiUsageLog l " +
           "WHERE l.idUsuario = :idUsuario " +
           "AND l.tipoConsulta = :tipo " +
           "AND l.fechaConsulta = :hoy")
    long contarConsultasHoy(@Param("idUsuario") Long idUsuario,
                            @Param("tipo") String tipo,
                            @Param("hoy") LocalDate hoy);
}

package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.AiResumenCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AiResumenCacheRepository extends JpaRepository<AiResumenCache, Long> {

    Optional<AiResumenCache> findByTipoDocAndIdDocumento(String tipoDoc, Long idDocumento);

    Optional<AiResumenCache> findByTipoDocAndIdDocumentoAndGeneradoEnAfter(
            String tipoDoc, Long idDocumento, LocalDateTime desde);
}

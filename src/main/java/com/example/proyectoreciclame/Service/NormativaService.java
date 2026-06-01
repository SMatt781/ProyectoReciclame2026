package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Dto.NormativaDetalleDTO;
import com.example.proyectoreciclame.Dto.NormativaResumenDTO;
import com.example.proyectoreciclame.Entity.Categoria;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.Repository.RegistroDescargaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NormativaService {

    @Autowired
    private NormativaRepository normativaRepository;

    @Autowired
    private RegistroDescargaRepository registroDescargaRepository;

    public NormativaDetalleDTO obtenerDetalleConContexto(Long idNormativa) {
        Normativa normativa = normativaRepository.findById(idNormativa).orElse(null);
        if (normativa == null) {
            return null;
        }

        // 1A. Normativas relacionadas (mismo organismo emisor)
        List<Normativa> relacionadasEntities = normativaRepository.findNormativasRelacionadas(
                normativa.getOrganismoEmisor(),
                idNormativa,
                PageRequest.of(0, 3)
        );
        List<NormativaResumenDTO> relacionadas = relacionadasEntities.stream()
                .map(NormativaResumenDTO::fromEntity)
                .collect(Collectors.toList());

        // 1B. Estadísticas
        long descargas = registroDescargaRepository.countByIdDocumentoAndTipoDocumento(idNormativa, "NORMATIVA");

        // 4. Normativas Similares — merge: categorías → organismo → año
        java.util.LinkedHashMap<Long, Normativa> vistos = new java.util.LinkedHashMap<>();

        // a) por categorías comunes (native SQL, más fiable)
        normativaRepository.findNormativasSimilaresPorCategoria(idNormativa)
                .forEach(n -> vistos.putIfAbsent(n.getIdNormativa(), n));

        // b) por organismo emisor
        if (vistos.size() < 4) {
            relacionadasEntities.forEach(n -> vistos.putIfAbsent(n.getIdNormativa(), n));
        }

        // c) por año como fallback
        if (vistos.size() < 4) {
            normativaRepository.findNormativasSimilaresPorAnio(normativa.getAnio(), idNormativa)
                    .forEach(n -> vistos.putIfAbsent(n.getIdNormativa(), n));
        }

        List<NormativaResumenDTO> similares = vistos.values().stream()
                .limit(4)
                .map(NormativaResumenDTO::fromEntity)
                .collect(Collectors.toList());

        return new NormativaDetalleDTO(normativa, relacionadas, similares, descargas);
    }
}

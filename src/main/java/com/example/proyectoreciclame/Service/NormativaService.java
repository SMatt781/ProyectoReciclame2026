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

        // 4. Normativas Similares
        List<Normativa> similaresEntities = new ArrayList<>();
        // Primero intentamos buscar del mismo organismo (usamos las mismas ya buscadas)
        if (!relacionadasEntities.isEmpty()) {
            int limite = Math.min(2, relacionadasEntities.size());
            for(int i = 0; i < limite; i++){
                similaresEntities.add(relacionadasEntities.get(i));
            }
        }
        
        // Completamos con categorias si faltan (hasta llegar a 3)
        if (similaresEntities.size() < 3 && normativa.getCategorias() != null && !normativa.getCategorias().isEmpty()) {
            List<Integer> idsCategorias = normativa.getCategorias().stream()
                    .map(Categoria::getIdCategoria)
                    .collect(Collectors.toList());
            List<Normativa> similaresPorCat = normativaRepository.findNormativasSimilaresPorCategoria(
                    idsCategorias,
                    idNormativa,
                    PageRequest.of(0, 3)
            );
            for (Normativa n : similaresPorCat) {
                if (similaresEntities.size() >= 3) break;
                if (similaresEntities.stream().noneMatch(s -> s.getIdNormativa().equals(n.getIdNormativa()))) {
                    similaresEntities.add(n);
                }
            }
        }
        
        List<NormativaResumenDTO> similares = similaresEntities.stream()
                .map(NormativaResumenDTO::fromEntity)
                .collect(Collectors.toList());

        return new NormativaDetalleDTO(normativa, relacionadas, similares, descargas);
    }
}

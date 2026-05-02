package com.example.proyectoreciclame.Dto;

import com.example.proyectoreciclame.Entity.Normativa;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NormativaDetalleDTO {
    private Normativa normativa;
    private List<NormativaResumenDTO> normativasRelacionadas;
    private List<NormativaResumenDTO> normativasSimilares;
    private long totalDescargas;

    public NormativaDetalleDTO(Normativa normativa, List<NormativaResumenDTO> normativasRelacionadas, List<NormativaResumenDTO> normativasSimilares, long totalDescargas) {
        this.normativa = normativa;
        this.normativasRelacionadas = normativasRelacionadas;
        this.normativasSimilares = normativasSimilares;
        this.totalDescargas = totalDescargas;
    }
}

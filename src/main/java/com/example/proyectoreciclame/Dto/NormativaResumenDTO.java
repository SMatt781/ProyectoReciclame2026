package com.example.proyectoreciclame.Dto;

import com.example.proyectoreciclame.Entity.Normativa;

public record NormativaResumenDTO(
        Long id,
        String titulo,
        String codigo,
        String acceso,
        String estado,
        Integer anio
) {
    public static NormativaResumenDTO fromEntity(Normativa n) {
        return new NormativaResumenDTO(
                n.getIdNormativa(),
                n.getTitulo(),
                n.getCodigo(),
                n.getAcceso() != null ? n.getAcceso().name() : "",
                n.getEstado() != null ? n.getEstado().name().replace("_", " ") : "",
                n.getAnio()
        );
    }
}

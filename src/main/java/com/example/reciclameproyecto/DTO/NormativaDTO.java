package com.example.reciclameproyecto.DTO;

import com.example.reciclameproyecto.entity.Normativa;

public record NormativaDTO(
                Long id,
                String titulo,
                String organismo,
                String codigo,
                String categoria,
                String estado,
                String acceso,
                String tipo,
                Integer anio,
                String obligatoriedad) {
        public static NormativaDTO fromEntity(Normativa n) {
                String categoriaNombre = (n.getCategorias() != null && !n.getCategorias().isEmpty())
                                ? n.getCategorias().get(0).getNombre()
                                : "Sin Categoría";

                return new NormativaDTO(
                                n.getIdNormativa(),
                                n.getTitulo(),
                                n.getOrganismoEmisor(),
                                n.getCodigo(),
                                categoriaNombre,
                                n.getEstado() != null ? n.getEstado().name().replace("_", " ") : "",
                                n.getAcceso() != null ? n.getAcceso().name() : "",
                                n.getTipoNorma() != null ? n.getTipoNorma().name() : "",
                                n.getAnio(),
                                n.getObligatoriedad());
        }
}

package com.example.proyectoreciclame.Dto;

import com.example.proyectoreciclame.Entity.Categoria;
import com.example.proyectoreciclame.Entity.Normativa;

import java.util.Collections;
import java.util.List;

public record NormativaDTO(
                Long id,
                String titulo,
                String organismo,
                String codigo,
                String categoria,
                List<String> categorias,
                String estado,
                String acceso,
                String alcance,
                String tipo,
                Integer anio,
                String obligatoriedad) {
        public static NormativaDTO fromEntity(Normativa n) {
                List<String> categorias = (n.getCategorias() != null && !n.getCategorias().isEmpty())
                                ? n.getCategorias().stream().map(Categoria::getNombre).toList()
                                : Collections.emptyList();

                String categoriaNombre = !categorias.isEmpty() ? categorias.get(0) : "Sin Categoría";

                return new NormativaDTO(
                                n.getIdNormativa(),
                                n.getTitulo(),
                                n.getOrganismoEmisor(),
                                n.getCodigo(),
                                categoriaNombre,
                                categorias,
                                n.getEstado() != null ? n.getEstado().name().replace("_", " ") : "",
                                n.getAcceso() != null ? n.getAcceso().name() : "",
                                n.getAlcance() != null ? n.getAlcance().name() : "",
                                n.getTipoNorma() != null ? n.getTipoNorma().name() : "",
                                n.getAnio(),
                                n.getObligatoriedad());
        }
}

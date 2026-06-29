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
        String obligatoriedad,
        List<CategoriaResumenDTO> categoriaDetalles) {
    public record CategoriaResumenDTO(String nombre, String codigo, String colorHex) {
    }

    public static NormativaDTO fromEntity(Normativa n) {
        List<String> categorias = (n.getCategorias() != null && !n.getCategorias().isEmpty())
                ? n.getCategorias().stream().map(Categoria::getNombre).toList()
                : Collections.emptyList();
        List<CategoriaResumenDTO> categoriaDetalles = (n.getCategorias() != null && !n.getCategorias().isEmpty())
                ? n.getCategorias().stream()
                .map(c -> new CategoriaResumenDTO(
                        c.getNombre(),
                        normalizarCodigoCategoria(c.getNombre(), c.getCodigo()),
                        c.getColorHex()))
                .toList()
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
                n.getObligatoriedad(),
                categoriaDetalles);
    }

    private static String normalizarCodigoCategoria(String nombre, String codigo) {
        if (codigo != null && !codigo.isBlank()) {
            return codigo;
        }

        String nombreBase = nombre != null ? nombre : "";
        String generado = java.util.Arrays.stream(nombreBase.split("\\s+"))
                .filter(s -> !s.isBlank())
                .limit(3)
                .map(s -> s.substring(0, 1).toUpperCase())
                .collect(java.util.stream.Collectors.joining());

        return generado.isBlank() ? "CAT" : generado;
    }
}

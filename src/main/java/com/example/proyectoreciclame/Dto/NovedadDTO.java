package com.example.proyectoreciclame.Dto;

import java.time.LocalDateTime;

public class NovedadDTO {
    private String tipo;
    private String nombre;
    private String accion;
    private String categoriaAbbr;
    private String color;
    private LocalDateTime fecha;
    private String fechaRelativa;

    public NovedadDTO(String tipo, String nombre, String accion, String categoriaAbbr, String color, LocalDateTime fecha, String fechaRelativa) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.accion = accion;
        this.categoriaAbbr = categoriaAbbr;
        this.color = color;
        this.fecha = fecha;
        this.fechaRelativa = fechaRelativa;
    }

    public String getTipo() { return tipo; }
    public String getNombre() { return nombre; }
    public String getAccion() { return accion; }
    public String getCategoriaAbbr() { return categoriaAbbr; }
    public String getColor() { return color; }
    public LocalDateTime getFecha() { return fecha; }
    public String getFechaRelativa() { return fechaRelativa; }
}

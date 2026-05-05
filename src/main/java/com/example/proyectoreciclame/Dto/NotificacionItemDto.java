package com.example.proyectoreciclame.Dto;

import java.time.LocalDateTime;

public class NotificacionItemDto {

    private String titulo;
    private String etiqueta;
    private String mensaje;
    private String tipo;
    private String enlace;
    private String tiempo;
    private LocalDateTime fecha;

    public NotificacionItemDto() {
    }

    public NotificacionItemDto(String titulo, String etiqueta, String mensaje, String tipo,
                               String enlace, String tiempo, LocalDateTime fecha) {
        this.titulo = titulo;
        this.etiqueta = etiqueta;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.enlace = enlace;
        this.tiempo = tiempo;
        this.fecha = fecha;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEnlace() {
        return enlace;
    }

    public void setEnlace(String enlace) {
        this.enlace = enlace;
    }

    public String getTiempo() {
        return tiempo;
    }

    public void setTiempo(String tiempo) {
        this.tiempo = tiempo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}


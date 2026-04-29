package com.example.proyectoreciclame.Dto;

public class SolicitudRegistroDto {

    private Long idUsuario;
    private String nombreCompleto;
    private String correo;
    private String dni;
    private String rolSolicitado;
    private String estado;
    private String fechaSolicitud;
    private String iniciales;

    public SolicitudRegistroDto(Long idUsuario, String nombreCompleto, String correo,
                                String dni, String rolSolicitado, String estado,
                                String fechaSolicitud, String iniciales) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.dni = dni;
        this.rolSolicitado = rolSolicitado;
        this.estado = estado;
        this.fechaSolicitud = fechaSolicitud;
        this.iniciales = iniciales;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public String getDni() {
        return dni;
    }

    public String getRolSolicitado() {
        return rolSolicitado;
    }

    public String getEstado() {
        return estado;
    }

    public String getFechaSolicitud() {
        return fechaSolicitud;
    }

    public String getIniciales() {
        return iniciales;
    }
}
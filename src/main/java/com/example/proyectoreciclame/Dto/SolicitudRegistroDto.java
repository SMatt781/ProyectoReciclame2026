package com.example.proyectoreciclame.Dto;

public class SolicitudRegistroDto {

    private Long idUsuario;
    private String nombreCompleto;
    private String correo;
    private String tipoIdentificacion;
    private String numeroIdentificacion;
    private String rolSolicitado;
    private String estado;
    private String fechaSolicitud;
    private String iniciales;

    public SolicitudRegistroDto(Long idUsuario, String nombreCompleto, String correo,
                                String tipoIdentificacion, String numeroIdentificacion,
                                String rolSolicitado, String estado,
                                String fechaSolicitud, String iniciales) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.tipoIdentificacion = tipoIdentificacion;
        this.numeroIdentificacion = numeroIdentificacion;
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

    public String getTipoIdentificacion() {
        return tipoIdentificacion;
    }

    public String getNumeroIdentificacion() {
        return numeroIdentificacion;
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

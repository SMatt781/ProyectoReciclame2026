package com.example.proyectoreciclame.Dto;

public class UsuarioGestionDto {

    private Long idUsuario;
    private String nombreCompleto;
    private String correo;
    private String tipoIdentificacion;
    private String numeroIdentificacion;
    private String empresa;
    private String rol;
    private String estado;
    private String fechaRegistro;
    private String iniciales;

    public UsuarioGestionDto(Long idUsuario, String nombreCompleto, String correo,
                             String tipoIdentificacion, String numeroIdentificacion,
                             String empresa, String rol, String estado, String fechaRegistro,
                             String iniciales) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.tipoIdentificacion = tipoIdentificacion;
        this.numeroIdentificacion = numeroIdentificacion;
        this.empresa = empresa;
        this.rol = rol;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
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

    public String getEmpresa() {
        return empresa;
    }

    public String getRol() {
        return rol;
    }

    public String getEstado() {
        return estado;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public String getIniciales() {
        return iniciales;
    }
}
package com.example.proyectoreciclame.Dto;

import jakarta.validation.constraints.NotBlank;

public class VerificarCodigoRecuperacionForm {

    @NotBlank(message = "Ingrese el código")
    private String correo;

    @NotBlank(message = "Ingrese el código")
    private String codigo;

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
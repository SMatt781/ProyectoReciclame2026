package com.example.proyectoreciclame.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RecuperarPasswordForm {

    @NotBlank(message = "Ingrese su correo")
    @Email(message = "Ingrese un correo válido")
    private String correo;

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
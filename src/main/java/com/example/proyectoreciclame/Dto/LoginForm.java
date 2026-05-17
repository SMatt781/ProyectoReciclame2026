package com.example.proyectoreciclame.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {

    @NotBlank(message = "Correo obligatorio")
    @Email(message = "Correo no válido")
    private String correo;

    @NotBlank(message = "Contraseña obligatoria")
    private String password;

    // getters y setters
}

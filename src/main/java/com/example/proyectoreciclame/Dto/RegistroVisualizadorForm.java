package com.example.proyectoreciclame.Dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class RegistroVisualizadorForm {

    @NotBlank(message = "Campo nombres obligatorio")
    private String nombres;

    @NotBlank(message = "Campo apellidos obligatorio")
    private String apellidos;

    @NotBlank(message = "Campo DNI obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
    private String dni;

    @NotBlank(message = "Campo teléfono obligatorio")
    private String telefono;

    @NotBlank(message = "Campo correo obligatorio")
    @Email(message = "Correo no válido")
    private String correo;

    @NotBlank(message = "Campo contraseña obligatorio")
    @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
    private String password;

    @NotBlank(message = "Debe confirmar la contraseña")
    private String confirmPassword;

    @AssertTrue(message = "Debe aceptar los términos")
    private boolean aceptaTerminos;

    // getters y setters
}
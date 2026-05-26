package com.example.proyectoreciclame.Dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroSocioForm {

    @NotBlank(message = "Campo nombres obligatorio")
    private String nombres;

    // Para DNI: apellidos obligatorios. Para RUC: opcionales (son NULL)
    private String apellidoPaterno;

    private String apellidoMaterno;

    // tipoIdentificacion: "DNI" o "RUC"
    @NotBlank(message = "Debe seleccionar tipo de identificación")
    private String tipoIdentificacion;

    @NotBlank(message = "Debe ingresar el número de identificación")
    private String numeroIdentificacion;

    @NotBlank(message = "Campo teléfono obligatorio")
    private String telefono;

    @NotBlank(message = "Campo correo obligatorio")
    @Email(message = "Correo no válido")
    private String correo;

    @NotBlank(message = "Campo contraseña obligatorio")
    private String password;

    @NotBlank(message = "Debe confirmar la contraseña")
    private String confirmPassword;

    @AssertTrue(message = "Debe aceptar los términos")
    private boolean aceptaTerminos;

    // Validación personalizada: formato según tipo seleccionado
    @AssertTrue(message = "El número de identificación no es válido (DNI: 8 dígitos, RUC: 11 dígitos)")
    public boolean isNumeroIdentificacionValido() {
        if (tipoIdentificacion == null || numeroIdentificacion == null) return false;
        if ("DNI".equals(tipoIdentificacion))  return numeroIdentificacion.matches("\\d{8}");
        if ("RUC".equals(tipoIdentificacion))  return numeroIdentificacion.matches("\\d{11}");
        return false;
    }
}
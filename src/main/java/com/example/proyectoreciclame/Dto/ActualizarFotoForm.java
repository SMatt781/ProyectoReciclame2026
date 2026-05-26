package com.example.proyectoreciclame.Dto;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para el formulario de actualización de foto de perfil
 */
public class ActualizarFotoForm {

    @NotNull(message = "Debe seleccionar una foto")
    private MultipartFile foto;

    public ActualizarFotoForm() {
    }

    public ActualizarFotoForm(MultipartFile foto) {
        this.foto = foto;
    }

    public MultipartFile getFoto() {
        return foto;
    }

    public void setFoto(MultipartFile foto) {
        this.foto = foto;
    }
}

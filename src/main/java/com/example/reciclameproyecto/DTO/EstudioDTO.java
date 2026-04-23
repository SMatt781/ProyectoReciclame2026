package com.example.reciclameproyecto.DTO;

import java.time.LocalDate;

public record EstudioDTO(
        Long id,
        String titulo,
        String descripcion,
        Integer anio,
        String formato,
        String estado,
        String tipoAcceso,
        LocalDate fechaPublicacion) {
}

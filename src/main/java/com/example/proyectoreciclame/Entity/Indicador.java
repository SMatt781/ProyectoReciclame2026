package com.example.proyectoreciclame.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "indicador")
public class Indicador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_indicador")
    private Integer idIndicador;

    @NotBlank
    @Size(max = 200)
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Size(max = 500)
    @Column(name = "descripcion")
    private String descripcion;

    @Size(max = 100)
    @Column(name = "categoria")
    private String categoria;

    @Size(max = 50)
    @Column(name = "unidad")
    private String unidad;

    @NotNull
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;
}

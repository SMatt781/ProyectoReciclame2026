package com.example.proyectoreciclame.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categoria", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"nombre", "tipo"})
})
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer idCategoria;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Size(max = 200)
    @Column(name = "descripcion")
    private String descripcion;

    @Size(max = 7)
    @Column(name = "color_hex")
    private String colorHex;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoCategoria tipo;

    @Size(max = 10)
    @Column(name = "codigo")
    private String codigo;

    @NotNull
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    public enum TipoCategoria {
        ESTUDIO, NORMATIVA
    }
}

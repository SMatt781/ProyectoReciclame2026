package com.example.reciclameproyecto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario_empresa")
public class UsuarioEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_empresa")
    private Long idUsuarioEmpresa;

    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @NotBlank
    @Size(min = 11, max = 11)
    @Column(name = "ruc", nullable = false)
    private String ruc;

    @NotBlank
    @Size(max = 200)
    @Column(name = "razon_social", nullable = false)
    private String razonSocial;

    @Size(max = 100)
    @Column(name = "cargo")
    private String cargo;
}

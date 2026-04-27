package com.example.proyectoreciclame.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario_empresa")
public class UsuarioEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_empresa")
    private Long idUsuarioEmpresa;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "ruc")
    private String ruc;

    @Column(name = "razon_social")
    private String razonSocial;

    @Column(name = "cargo")
    private String cargo;

    public Long getIdUsuarioEmpresa() {
        return idUsuarioEmpresa;
    }

    public void setIdUsuarioEmpresa(Long idUsuarioEmpresa) {
        this.idUsuarioEmpresa = idUsuarioEmpresa;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }
}
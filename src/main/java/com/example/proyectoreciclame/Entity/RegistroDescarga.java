package com.example.proyectoreciclame.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "registro_descarga")
public class RegistroDescarga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_descarga")
    private Long idDescarga;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo_documento")
    private String tipoDocumento;

    @Column(name = "id_documento")
    private Long idDocumento;

    @Column(name = "nombre_documento")
    private String nombreDocumento;

    @Column(name = "url_descargada")
    private String urlDescargada;

    @Column(name = "fecha_descarga")
    private LocalDateTime fechaDescarga;

    public Long getIdDescarga() {
        return idDescarga;
    }

    public void setIdDescarga(Long idDescarga) {
        this.idDescarga = idDescarga;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public Long getIdDocumento() {
        return idDocumento;
    }

    public void setIdDocumento(Long idDocumento) {
        this.idDocumento = idDocumento;
    }

    public String getNombreDocumento() {
        return nombreDocumento;
    }

    public void setNombreDocumento(String nombreDocumento) {
        this.nombreDocumento = nombreDocumento;
    }

    public String getUrlDescargada() {
        return urlDescargada;
    }

    public void setUrlDescargada(String urlDescargada) {
        this.urlDescargada = urlDescargada;
    }

    public LocalDateTime getFechaDescarga() {
        return fechaDescarga;
    }

    public void setFechaDescarga(LocalDateTime fechaDescarga) {
        this.fechaDescarga = fechaDescarga;
    }

    @Transient
    public String getUsuarioNombreCompleto() {
        if (usuario == null) {
            return "";
        }
        String am = usuario.getApellidoMaterno() != null ? " " + usuario.getApellidoMaterno() : "";
        return (usuario.getNombres() + " " + usuario.getApellidoPaterno() + am).trim();
    }

    @Transient
    public String getUsuarioIniciales() {
        if (usuario == null) {
            return "";
        }
        String n1 = usuario.getNombres() != null && !usuario.getNombres().isBlank()
                ? usuario.getNombres().substring(0, 1).toUpperCase()
                : "";
        String a1 = usuario.getApellidoPaterno() != null && !usuario.getApellidoPaterno().isBlank()
                ? usuario.getApellidoPaterno().substring(0, 1).toUpperCase()
                : "";
        return n1 + a1;
    }

    @Transient
    public String getFechaTexto() {
        if (fechaDescarga == null) {
            return "-";
        }
        return fechaDescarga.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    @Transient
    public String getHoraTexto() {
        if (fechaDescarga == null) {
            return "-";
        }
        return fechaDescarga.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
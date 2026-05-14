package com.example.proyectoreciclame.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contenido_guardado")
public class ContenidoGuardado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_guardado")
    private Long idGuardado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo_documento", nullable = false)
    private String tipoDocumento;

    @Column(name = "id_documento", nullable = false)
    private Long idDocumento;

    @Column(name = "nombre_documento", nullable = false)
    private String nombreDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_carpeta")
    private EspacioCarpeta carpeta;

    @Column(name = "fecha_guardado", nullable = false)
    private LocalDateTime fechaGuardado = LocalDateTime.now();

    public ContenidoGuardado() {}

    public Long getIdGuardado() { return idGuardado; }
    public void setIdGuardado(Long idGuardado) { this.idGuardado = idGuardado; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public Long getIdDocumento() { return idDocumento; }
    public void setIdDocumento(Long idDocumento) { this.idDocumento = idDocumento; }

    public String getNombreDocumento() { return nombreDocumento; }
    public void setNombreDocumento(String nombreDocumento) { this.nombreDocumento = nombreDocumento; }

    public EspacioCarpeta getCarpeta() { return carpeta; }
    public void setCarpeta(EspacioCarpeta carpeta) { this.carpeta = carpeta; }

    public LocalDateTime getFechaGuardado() { return fechaGuardado; }
    public void setFechaGuardado(LocalDateTime fechaGuardado) { this.fechaGuardado = fechaGuardado; }
}

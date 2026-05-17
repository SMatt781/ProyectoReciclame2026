package com.example.proyectoreciclame.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_lectura")
public class HistorialLectura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lectura")
    private Long idLectura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo_documento", nullable = false)
    private String tipoDocumento;

    @Column(name = "id_documento", nullable = false)
    private Long idDocumento;

    @Column(name = "nombre_documento", nullable = false)
    private String nombreDocumento;

    @Column(name = "veces_visto", nullable = false)
    private Integer vecesVisto = 1;

    @Column(name = "primera_lectura", nullable = false, updatable = false)
    private LocalDateTime primeraLectura = LocalDateTime.now();

    @Column(name = "ultima_lectura", nullable = false)
    private LocalDateTime ultimaLectura = LocalDateTime.now();

    public HistorialLectura() {}

    public Long getIdLectura() { return idLectura; }
    public void setIdLectura(Long idLectura) { this.idLectura = idLectura; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public Long getIdDocumento() { return idDocumento; }
    public void setIdDocumento(Long idDocumento) { this.idDocumento = idDocumento; }

    public String getNombreDocumento() { return nombreDocumento; }
    public void setNombreDocumento(String nombreDocumento) { this.nombreDocumento = nombreDocumento; }

    public Integer getVecesVisto() { return vecesVisto; }
    public void setVecesVisto(Integer vecesVisto) { this.vecesVisto = vecesVisto; }

    public LocalDateTime getPrimeraLectura() { return primeraLectura; }
    public void setPrimeraLectura(LocalDateTime primeraLectura) { this.primeraLectura = primeraLectura; }

    public LocalDateTime getUltimaLectura() { return ultimaLectura; }
    public void setUltimaLectura(LocalDateTime ultimaLectura) { this.ultimaLectura = ultimaLectura; }
}

package com.example.proyectoreciclame.Entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "registro_sesiones")
public class RegistroSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Long idSesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "token")
    private String token;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Column(name = "ip")
    private String ip;

    @Column(name = "agente_usuario")
    private String agenteUsuario;

    @Column(name = "estado")
    private String estado;

    public Long getIdSesion() {
        return idSesion;
    }

    public void setIdSesion(Long idSesion) {
        this.idSesion = idSesion;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAgenteUsuario() {
        return agenteUsuario;
    }

    public void setAgenteUsuario(String agenteUsuario) {
        this.agenteUsuario = agenteUsuario;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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
        if (fechaInicio == null) {
            return "-";
        }
        return fechaInicio.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    @Transient
    public String getHoraEntradaTexto() {
        if (fechaInicio == null) {
            return "-";
        }
        return fechaInicio.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    @Transient
    public String getDuracionTexto() {
        Integer minutosTotales = duracionMinutos;

        if (minutosTotales == null && fechaInicio != null) {
            LocalDateTime fin = fechaFin != null ? fechaFin : LocalDateTime.now();
            minutosTotales = (int) Duration.between(fechaInicio, fin).toMinutes();
        }

        if (minutosTotales == null || minutosTotales < 0) {
            return "-";
        }

        long horas = minutosTotales / 60;
        long minutos = minutosTotales % 60;

        return String.format("%02dh %02dm", horas, minutos);
    }
}
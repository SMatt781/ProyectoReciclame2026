package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Entity.RegistroDescarga;
import com.example.proyectoreciclame.Entity.RegistroSesion;
import com.example.proyectoreciclame.Repository.RegistroDescargaRepository;
import com.example.proyectoreciclame.Repository.RegistroSesionRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/registros")
public class AdminRegistroController {

    private final RegistroSesionRepository registroSesionRepository;
    private final RegistroDescargaRepository registroDescargaRepository;

    public AdminRegistroController(RegistroSesionRepository registroSesionRepository,
                                   RegistroDescargaRepository registroDescargaRepository) {
        this.registroSesionRepository = registroSesionRepository;
        this.registroDescargaRepository = registroDescargaRepository;
    }

    @GetMapping("/actividad")
    public String actividad(@RequestParam(required = false) String texto,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                            @RequestParam(required = false) String rol,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {

        String textoLimpio = limpiar(texto);
        String rolLimpio = limpiar(rol);

        LocalDateTime inicio = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime fin = fecha != null ? fecha.atTime(23, 59, 59) : null;

        Pageable pageable = PageRequest.of(page, 3);
        Page<RegistroSesion> pagina = registroSesionRepository.buscarFiltrado(
                textoLimpio, inicio, fin, rolLimpio, pageable
        );

        model.addAttribute("sesiones", pagina.getContent());
        model.addAttribute("currentSection", "admin-registros-actividad");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pagina.getTotalPages());
        model.addAttribute("hasPrevious", pagina.hasPrevious());
        model.addAttribute("hasNext", pagina.hasNext());

        model.addAttribute("texto", texto);
        model.addAttribute("fecha", fecha);
        model.addAttribute("rol", rol);

        return "admin/registros-actividad";
    }

    @GetMapping("/descargas")
    public String descargas(@RequestParam(required = false) String texto,
                            @RequestParam(required = false) String tipo,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {

        String textoLimpio = limpiar(texto);
        String tipoLimpio = limpiar(tipo);

        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

        Pageable pageable = PageRequest.of(page, 3);
        Page<RegistroDescarga> pagina = registroDescargaRepository.buscarFiltrado(
                textoLimpio, tipoLimpio, inicio, fin, pageable
        );

        model.addAttribute("descargas", pagina.getContent());
        model.addAttribute("currentSection", "admin-registros-descargas");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pagina.getTotalPages());
        model.addAttribute("hasPrevious", pagina.hasPrevious());
        model.addAttribute("hasNext", pagina.hasNext());

        model.addAttribute("texto", texto);
        model.addAttribute("tipo", tipo);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        model.addAttribute("totalDescargas", registroDescargaRepository.count());
        model.addAttribute("totalNormativas", registroDescargaRepository.countByTipoDocumento("NORMATIVA"));
        model.addAttribute("totalEstudios", registroDescargaRepository.countByTipoDocumento("ESTUDIO"));

        return "admin/registros-descargas";
    }

    @GetMapping("/actividad/exportar")
    public void exportarActividad(@RequestParam(required = false) String texto,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                  @RequestParam(required = false) String rol,
                                  HttpServletResponse response) throws Exception {

        String textoLimpio = limpiar(texto);
        String rolLimpio = limpiar(rol);

        LocalDateTime inicio = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime fin = fecha != null ? fecha.atTime(23, 59, 59) : null;

        Page<RegistroSesion> pagina = registroSesionRepository.buscarFiltrado(
                textoLimpio, inicio, fin, rolLimpio, PageRequest.of(0, 10000)
        );

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=registros_actividad.csv");

        PrintWriter writer = response.getWriter();
        writer.println("Usuario,Rol,Fecha,Hora Entrada,Duración,Estado");

        for (RegistroSesion r : pagina.getContent()) {
            String nombre = r.getUsuarioNombreCompleto();
            String nombreRol = r.getUsuario() != null && r.getUsuario().getRol() != null
                    ? r.getUsuario().getRol().getNombre()
                    : "";
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    nombre,
                    nombreRol,
                    r.getFechaTexto(),
                    r.getHoraEntradaTexto(),
                    r.getDuracionTexto(),
                    r.getEstado()
            );
        }
        writer.flush();
    }

    @GetMapping("/descargas/exportar")
    public void exportarDescargas(@RequestParam(required = false) String texto,
                                  @RequestParam(required = false) String tipo,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                                  HttpServletResponse response) throws Exception {

        String textoLimpio = limpiar(texto);
        String tipoLimpio = limpiar(tipo);

        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

        Page<RegistroDescarga> pagina = registroDescargaRepository.buscarFiltrado(
                textoLimpio, tipoLimpio, inicio, fin, PageRequest.of(0, 10000)
        );

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=registros_descargas.csv");

        PrintWriter writer = response.getWriter();
        writer.println("Usuario,Documento,Sección,Fecha,Hora");

        for (RegistroDescarga r : pagina.getContent()) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    r.getUsuarioNombreCompleto(),
                    r.getNombreDocumento(),
                    r.getTipoDocumento(),
                    r.getFechaTexto(),
                    r.getHoraTexto()
            );
        }
        writer.flush();
    }

    private String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
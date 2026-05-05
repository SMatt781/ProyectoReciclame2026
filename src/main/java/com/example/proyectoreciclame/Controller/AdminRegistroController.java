package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Entity.RegistroDescarga;
import com.example.proyectoreciclame.Entity.RegistroSesion;
import com.example.proyectoreciclame.Repository.RegistroDescargaRepository;
import com.example.proyectoreciclame.Repository.RegistroSesionRepository;
import com.example.proyectoreciclame.util.PaginationUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

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

        model.addAttribute(
                "pageNumbers",
                PaginationUtils.buildPageNumbers(page, pagina.getTotalPages())
        );

        model.addAttribute("texto", texto);
        model.addAttribute("fecha", fecha);
        model.addAttribute("rol", rol);

        Double promedio = registroSesionRepository.getPromedioDuracion();
        model.addAttribute("promedioDuracion", promedio != null ? Math.round(promedio) : 0);
        model.addAttribute("ultimaSesion", registroSesionRepository.getUltimaSesion());
        model.addAttribute("totalActivos", registroSesionRepository.countUsuariosActivos());
        model.addAttribute("usuarioFrecuente", registroSesionRepository.getUsuarioMasFrecuente());

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

        model.addAttribute(
                "pageNumbers",
                PaginationUtils.buildPageNumbers(page, pagina.getTotalPages())
        );

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

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Actividad");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Usuario");
        header.createCell(1).setCellValue("Rol");
        header.createCell(2).setCellValue("Fecha");
        header.createCell(3).setCellValue("Hora Entrada");
        header.createCell(4).setCellValue("Duración");
        header.createCell(5).setCellValue("Estado");

        int rowNum = 1;

        for (RegistroSesion r : pagina.getContent()) {
            String nombre = r.getUsuarioNombreCompleto();

            String nombreRol = r.getUsuario() != null && r.getUsuario().getRol() != null
                    ? r.getUsuario().getRol().getNombre()
                    : "";

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(nombre);
            row.createCell(1).setCellValue(nombreRol);
            row.createCell(2).setCellValue(r.getFechaTexto());
            row.createCell(3).setCellValue(r.getHoraEntradaTexto());
            row.createCell(4).setCellValue(r.getDuracionTexto());
            row.createCell(5).setCellValue(r.getEstado());
        }

        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=registros_actividad.xlsx");

        workbook.write(response.getOutputStream());
        workbook.close();
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

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Descargas");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Usuario");
        header.createCell(1).setCellValue("Documento");
        header.createCell(2).setCellValue("Sección");
        header.createCell(3).setCellValue("Fecha");
        header.createCell(4).setCellValue("Hora");

        int rowNum = 1;

        for (RegistroDescarga r : pagina.getContent()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getUsuarioNombreCompleto());
            row.createCell(1).setCellValue(r.getNombreDocumento());
            row.createCell(2).setCellValue(r.getTipoDocumento());
            row.createCell(3).setCellValue(r.getFechaTexto());
            row.createCell(4).setCellValue(r.getHoraTexto());
        }

        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=registros_descargas.xlsx");

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
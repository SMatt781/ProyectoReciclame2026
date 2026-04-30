package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.Repository.CategoriaRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class AdminEstudiosController {

    @Autowired
    private EstudioRepository estudioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/admin/estudios")
    public String estudiosAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "yearSelect") Integer anio,
            @RequestParam(required = false, name = "dateStart") String dateStartStr,
            @RequestParam(required = false, name = "dateEnd") String dateEndStr,
            @RequestParam(required = false) java.util.List<String> format,
            @RequestParam(required = false) java.util.List<String> estado,
            Model model
    ) {
        java.time.LocalDate fechaInicio = (dateStartStr != null && !dateStartStr.isBlank())
                ? java.time.LocalDate.parse(dateStartStr)
                : null;

        java.time.LocalDate fechaFin = (dateEndStr != null && !dateEndStr.isBlank())
                ? java.time.LocalDate.parse(dateEndStr)
                : null;

        boolean hasFormatos = format != null && !format.isEmpty();
        boolean hasEstados = estado != null && !estado.isEmpty();

        model.addAttribute("currentSection", "admin-estudios");
        model.addAttribute("currentPage", "estudios");

        model.addAttribute("estudios", estudioRepository.filtrarAdmin(
                (search != null && !search.isBlank()) ? search : null,
                anio,
                fechaInicio,
                fechaFin,
                hasFormatos,
                hasFormatos ? format : java.util.List.of(),
                hasEstados,
                hasEstados ? estado : java.util.List.of()
        ));
        model.addAttribute("search", search);
        model.addAttribute("anioSeleccionado", anio);
        model.addAttribute("fechaInicioSeleccionada", dateStartStr);
        model.addAttribute("fechaFinSeleccionada", dateEndStr);
        model.addAttribute("formatosSeleccionados", format);
        model.addAttribute("estadosSeleccionados", estado);

        return "admin/estudio_main_admin";
    }
    @PostMapping("/admin/estudio_new")
    public String guardarEstudio(
            @RequestParam("nombre") String nombre,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("anio") Integer anio,
            @RequestParam("formato") String formato,
            @RequestParam(value = "archivo", required = false) org.springframework.web.multipart.MultipartFile archivo,
            org.springframework.web.servlet.mvc.support.RedirectAttributes attr
    ) {

        try {

            com.example.proyectoreciclame.Entity.Estudio estudio = new com.example.proyectoreciclame.Entity.Estudio();

            // 🔹 datos básicos
            estudio.setTitulo(nombre);
            estudio.setDescripcion(descripcion);
            estudio.setAnio(anio);

            // 🔹 formato (ENUM)
            if (formato.toUpperCase().contains("PDF")) {
                estudio.setFormato(com.example.proyectoreciclame.Entity.Estudio.FormatoEstudio.PDF);
            } else {
                estudio.setFormato(com.example.proyectoreciclame.Entity.Estudio.FormatoEstudio.PPTX);
            }

            // 🔹 valores por defecto
            estudio.setEstado(com.example.proyectoreciclame.Entity.Estudio.EstadoEstudio.BORRADOR);
            estudio.setTipoAcceso(com.example.proyectoreciclame.Entity.Estudio.TipoAcceso.DESCARGA);
            estudio.setFechaPublicacion(java.time.LocalDate.now());
            estudio.setIndiceRelevancia(Integer.valueOf(1));

            // 🔴 CRÍTICO: usuario creador
            com.example.proyectoreciclame.Entity.Usuario usuario =
                    usuarioRepository.findById(Long.valueOf(1))
                            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            estudio.setUsuarioCreador(usuario);

            // 🔹 archivo
            // 🔹 archivo real PDF / PPTX
            if (archivo != null && !archivo.isEmpty()) {

                String nombreOriginal = archivo.getOriginalFilename();
                String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();

                if (!extension.equals(".pdf") && !extension.equals(".pptx")) {
                    throw new RuntimeException("Solo se permiten archivos PDF o PPTX");
                }

                String nombreArchivo = System.currentTimeMillis() + "_" + nombreOriginal;

                String ruta = "src/main/resources/static/uploads/estudios/";

                java.io.File carpeta = new java.io.File(ruta);
                if (!carpeta.exists()) {
                    carpeta.mkdirs();
                }

                java.io.File destino = new java.io.File(ruta + nombreArchivo);
                archivo.transferTo(destino);

                estudio.setArchivoNombre(nombreArchivo);
                estudio.setArchivoTamanioKb(Integer.valueOf((int) (archivo.getSize() / 1024)));
                estudio.setArchivoUrl("/uploads/estudios/" + nombreArchivo);
            }
            java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

            estudio.setFechaCreacion(ahora);
            estudio.setFechaActualizacion(ahora);

            estudioRepository.save(estudio);

            attr.addFlashAttribute("msg", "Estudio creado correctamente");

        } catch (Exception e) {
            attr.addFlashAttribute("msgError", "Error: " + e.getMessage());
        }

        return "redirect:/admin/estudios";
    }

    @GetMapping("/admin/normativas")

    public String normativasAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "yearSelect") Integer anio,
            @RequestParam(required = false, name = "dateStart") String dateStartStr,
            @RequestParam(required = false, name = "dateEnd") String dateEndStr,
            @RequestParam(required = false) java.util.List<String> categoria,
            @RequestParam(required = false) java.util.List<String> estado,
            @RequestParam(required = false) java.util.List<String> acceso,
            @RequestParam(required = false) java.util.List<String> alcance,
            @RequestParam(required = false) java.util.List<String> obligatoriedad,
            Model model
    ) {
        java.time.LocalDateTime fechaInicio = (dateStartStr != null && !dateStartStr.isBlank())
                ? java.time.LocalDate.parse(dateStartStr).atStartOfDay()
                : null;

        java.time.LocalDateTime fechaFin = (dateEndStr != null && !dateEndStr.isBlank())
                ? java.time.LocalDate.parse(dateEndStr).atTime(23, 59, 59)
                : null;

        boolean hasCategoria = categoria != null && !categoria.isEmpty();
        boolean hasEstado = estado != null && !estado.isEmpty();
        boolean hasAcceso = acceso != null && !acceso.isEmpty();
        boolean hasAlcance = alcance != null && !alcance.isEmpty();
        boolean hasObligatoriedad = obligatoriedad != null && !obligatoriedad.isEmpty();

        model.addAttribute("currentPage", "repoNormativo");
        model.addAttribute("currentSection", "admin-normativas");

        if (search != null && !search.isBlank()) {
            model.addAttribute("normativas", normativaRepository.findByKeyword(search));
        } else {
            model.addAttribute("normativas", normativaRepository.findWithAdvancedFilters(
                    anio != null, anio,
                    fechaInicio,
                    fechaFin,
                    hasEstado, hasEstado ? estado : java.util.List.of(),
                    hasAcceso, hasAcceso ? acceso : java.util.List.of(),
                    hasAlcance, hasAlcance ? alcance : java.util.List.of(),
                    hasObligatoriedad, hasObligatoriedad ? obligatoriedad : java.util.List.of(),
                    hasCategoria, hasCategoria ? categoria : java.util.List.of()
            ));
        }

        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedYear", anio);
        model.addAttribute("dateStart", dateStartStr);
        model.addAttribute("dateEnd", dateEndStr);
        model.addAttribute("selectedCategorias", categoria);
        model.addAttribute("selectedEstados", estado);
        model.addAttribute("selectedAccesos", acceso);
        model.addAttribute("selectedAlcances", alcance);
        model.addAttribute("selectedObligatoriedades", obligatoriedad);

        return "admin/repo_main_admin";
    }

    @PostMapping("/admin/repo_new")
    public String guardarNormativa(
            @RequestParam(value = "titulo", required = false) String titulo,
            @RequestParam(value = "organismoEmisor", required = false) String organismoEmisor,
            @RequestParam(value = "codigo", required = false) String codigo,
            @RequestParam(value = "anio", required = false) Integer anio,
            @RequestParam(value = "tipoNorma", required = false) String tipoNorma,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "archivo", required = false) org.springframework.web.multipart.MultipartFile archivo,
            @RequestParam(value = "enlace", required = false) String enlace,
            org.springframework.web.servlet.mvc.support.RedirectAttributes attr
    ) {
        try {
            if (titulo == null || titulo.isBlank()) {
                throw new RuntimeException("Debe ingresar el nombre de la normativa");
            }

            if (organismoEmisor == null || organismoEmisor.isBlank()) {
                throw new RuntimeException("Debe ingresar el organismo emisor");
            }

            if (anio == null) {
                anio = java.time.LocalDate.now().getYear();
            }

            com.example.proyectoreciclame.Entity.Normativa n =
                    new com.example.proyectoreciclame.Entity.Normativa();

            n.setTitulo(titulo);
            n.setOrganismoEmisor(organismoEmisor);
            n.setCodigo(codigo);
            n.setAnio(anio);

            n.setTipoNorma(com.example.proyectoreciclame.Entity.Normativa.TipoNorma.valueOf(
                    tipoNorma != null && !tipoNorma.isBlank() ? tipoNorma : "DECRETO_LEY"
            ));

            n.setEstado(com.example.proyectoreciclame.Entity.Normativa.EstadoNormativa.valueOf(
                    estado != null && !estado.isBlank() ? estado : "VIGENTE"
            ));

            n.setAcceso(com.example.proyectoreciclame.Entity.Normativa.AccesoNormativa.GRATIS);
            n.setAlcance(com.example.proyectoreciclame.Entity.Normativa.AlcanceNormativa.NACIONAL);
            n.setObligatoriedad("Legal Vinculante");

            com.example.proyectoreciclame.Entity.Usuario usuario =
                    usuarioRepository.findById(1L)
                            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            n.setUsuarioCreador(usuario);

            if (archivo != null && !archivo.isEmpty()) {
                String nombreOriginal = archivo.getOriginalFilename();
                String nombreArchivo = System.currentTimeMillis() + "_" + nombreOriginal;

                String ruta = "src/main/resources/static/uploads/normativas/";

                java.io.File carpeta = new java.io.File(ruta);
                if (!carpeta.exists()) carpeta.mkdirs();

                java.io.File destino = new java.io.File(ruta + nombreArchivo);
                archivo.transferTo(destino);

                n.setArchivoNombre(nombreArchivo);
                n.setArchivoUrl("/uploads/normativas/" + nombreArchivo);
            }

            if (enlace != null && !enlace.isBlank()) {
                n.setEnlaceExterno(enlace);
            }

            java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
            n.setFechaCreacion(ahora);
            n.setFechaActualizacion(ahora);

            normativaRepository.save(n);

            attr.addFlashAttribute("msg", "Normativa creada correctamente");

        } catch (Exception e) {
            attr.addFlashAttribute("msgError", "Error: " + e.getMessage());
        }
        System.out.println("GUARDANDO NORMATIVA...");
        System.out.println("titulo = " + titulo);
        return "redirect:/admin/normativas";


    }
    @GetMapping("/admin/estudios/exportar")
    public org.springframework.http.ResponseEntity<byte[]> exportarEstudios() throws java.io.IOException {

        java.util.List<com.example.proyectoreciclame.Entity.Estudio> estudios = estudioRepository.findAll();

        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Estudios");

        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Nombre");
        header.createCell(1).setCellValue("Estado");
        header.createCell(2).setCellValue("Formato");
        header.createCell(3).setCellValue("Año");


        int rowNum = 1;

        for (com.example.proyectoreciclame.Entity.Estudio e : estudios) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(e.getTitulo());
            row.createCell(1).setCellValue(e.getEstado().name());
            row.createCell(2).setCellValue(e.getFormato().name());
            row.createCell(3).setCellValue(e.getAnio());

        }

        for (int i = 0; i <= 3; i++) {
            sheet.autoSizeColumn(i);
        }

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=estudios.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(out.toByteArray());
    }

    @GetMapping("/admin/estudios/{id}")
    public String visualizarEstudioAdmin(@PathVariable Long id, Model model) {

        Estudio estudio = estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado"));

        model.addAttribute("estudio", estudio);
        model.addAttribute("currentSection", "admin-estudios");

        return "admin/visualizar_estudio_admin";
    }

    @GetMapping("/admin/estudios/{id}/descargar")
    public ResponseEntity<Resource> descargarEstudio(@PathVariable Long id) throws IOException {

        Estudio estudio = estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado"));

        Path path = Paths.get("uploads/estudios").resolve(estudio.getArchivoNombre());
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + estudio.getArchivoNombre() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
    @Autowired
    private com.example.proyectoreciclame.Repository.NormativaRepository normativaRepository;

    @Autowired
    private com.example.proyectoreciclame.Repository.CategoriaRepository categoriaRepository;

    @GetMapping("/admin/normativas/{id}")
    public String verNormativa(@PathVariable Long id, Model model) {
        Normativa normativa = normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada"));

        model.addAttribute("normativa", normativa);
        model.addAttribute("currentSection", "admin-normativas");

        return "admin/repo_main_admin";
    }
    @GetMapping("/admin/normativas/{id}/eliminar")
    public String eliminarNormativa(@PathVariable Long id,
                                    RedirectAttributes attr) {
        normativaRepository.deleteById(id);
        attr.addFlashAttribute("msg", "Normativa eliminada correctamente");
        return "redirect:/admin/normativas";
    }

    @GetMapping("/admin/normativas/{id}/editar")
    public String editarNormativa(@PathVariable Long id, Model model) {
        Normativa normativa = normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada"));

        model.addAttribute("normativaEdit", normativa);
        model.addAttribute("normativas", normativaRepository.findAllNormativas());
        model.addAttribute("currentPage", "repoNormativo");
        model.addAttribute("currentSection", "admin-normativas");

        return "admin/repo_main_admin";
    }
    @PostMapping("/admin/normativas/{id}/actualizar")
    public String actualizarNormativa(
            @PathVariable Long id,
            @RequestParam String titulo,
            @RequestParam String organismoEmisor,
            @RequestParam String codigo,
            @RequestParam Integer anio,
            @RequestParam String tipoNorma,
            @RequestParam String estado,
            RedirectAttributes attr
    ) {
        Normativa n = normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada"));

        n.setTitulo(titulo);
        n.setOrganismoEmisor(organismoEmisor);
        n.setCodigo(codigo);
        n.setAnio(anio);
        n.setTipoNorma(Normativa.TipoNorma.valueOf(tipoNorma));
        n.setEstado(Normativa.EstadoNormativa.valueOf(estado));
        n.setFechaActualizacion(java.time.LocalDateTime.now());

        normativaRepository.save(n);

        attr.addFlashAttribute("msg", "Normativa actualizada correctamente");
        return "redirect:/admin/normativas";
    }



}
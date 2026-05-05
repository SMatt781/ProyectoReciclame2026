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
import java.util.List;

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
            @RequestParam(value = "estado", required = false) String estado,
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
            estudio.setEstado(Estudio.EstadoEstudio.valueOf(
                    estado != null && !estado.isBlank() ? estado : "BORRADOR"
            ));
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

        // 🔥 OBTENER TODAS LAS NORMATIVAS (para dashboard)
        java.util.List<Normativa> lista = normativaRepository.findAllNormativas();

// TOTAL
        long totalNormas = lista.size();
        model.addAttribute("totalNormas", totalNormas);

// =====================
// 🔵 DISTRIBUCIÓN POR TEMA
// =====================

        long countEc = lista.stream()
                .filter(n -> n.getCategorias().stream()
                        .anyMatch(c -> c.getNombre().equalsIgnoreCase("Economía circular")))
                .count();

        long countGr = lista.stream()
                .filter(n -> n.getCategorias().stream()
                        .anyMatch(c -> c.getNombre().equalsIgnoreCase("Gestión de residuos")))
                .count();

        long countEe = lista.stream()
                .filter(n -> n.getCategorias().stream()
                        .anyMatch(c -> c.getNombre().equalsIgnoreCase("Envases y Embalajes")))
                .count();

        long countRep = lista.stream()
                .filter(n -> n.getCategorias() != null && n.getCategorias().stream()
                        .anyMatch(c -> c.getNombre() != null &&
                                c.getNombre().equalsIgnoreCase("Responsabilidad Extendida")))
                .count();

        long countOtro = lista.stream()
                .filter(n -> n.getCategorias() != null)
                .flatMap(n -> n.getCategorias().stream())
                .filter(c -> c.getNombre() != null)
                .filter(c ->
                        !c.getNombre().equalsIgnoreCase("Economía circular") &&
                                !c.getNombre().equalsIgnoreCase("Gestión de residuos") &&
                                !c.getNombre().equalsIgnoreCase("Envases y Embalajes") &&
                                !c.getNombre().equalsIgnoreCase("Responsabilidad Extendida")
                )
                .count();

        long sinCategoria = lista.stream()
                .filter(n -> n.getCategorias() == null || n.getCategorias().isEmpty())
                .count();

        countOtro = countOtro + sinCategoria;
        long totalTemas = countEc + countGr + countEe + countRep + countOtro;

// porcentajes
        double pctEc = totalNormas > 0 ? (countEc * 100.0 / totalTemas) : 0;
        double pctGr = totalNormas > 0 ? (countGr * 100.0 / totalTemas) : 0;
        double pctEe = totalNormas > 0 ? (countEe * 100.0 / totalTemas) : 0;
        double pctRep = totalNormas > 0 ? (countRep * 100.0 / totalTemas) : 0;
        double pctOtro = totalNormas > 0 ? (countOtro * 100.0 / totalTemas) : 0;
        double circ = 282.74;

        double dashEc = totalNormas > 0 ? (countEc * circ / totalTemas) : 0;
        double dashGr = totalNormas > 0 ? (countGr * circ / totalTemas) : 0;
        double dashEe = totalNormas > 0 ? (countEe * circ / totalTemas) : 0;
        double dashRep = totalNormas > 0 ? (countRep * circ / totalTemas) : 0;
        double dashOtro = totalNormas > 0 ? (countOtro * circ / totalTemas) : 0;

        double startEc = -90;
        double startGr = startEc + (pctEc * 3.6);
        double startEe = startGr + (pctGr * 3.6);
        double startRep = startEe + (pctEe * 3.6);
        double startOtro = startRep + (pctRep * 3.6);

        model.addAttribute("dashEc", dashEc + " " + circ);
        model.addAttribute("dashGr", dashGr + " " + circ);
        model.addAttribute("dashEe", dashEe + " " + circ);
        model.addAttribute("dashRep", dashRep + " " + circ);
        model.addAttribute("dashOtro", dashOtro + " " + circ);

        model.addAttribute("startEc", startEc);
        model.addAttribute("startGr", startGr);
        model.addAttribute("startEe", startEe);
        model.addAttribute("startRep", startRep);
        model.addAttribute("startOtro", startOtro);

// enviar al model
        model.addAttribute("countEc", countEc);
        model.addAttribute("countGr", countGr);
        model.addAttribute("countEe", countEe);
        model.addAttribute("countRep", countRep);
        model.addAttribute("countOtro", countOtro);

        model.addAttribute("pctEc", pctEc);
        model.addAttribute("pctGr", pctGr);
        model.addAttribute("pctEe", pctEe);
        model.addAttribute("pctRep", pctRep);
        model.addAttribute("pctOtro", pctOtro);

// =====================
// 🟢 ESTADO POR ALCANCE
// =====================

        long nacTotal = lista.stream().filter(n -> n.getAlcance().name().equals("NACIONAL")).count();
        long intTotal = lista.stream().filter(n -> n.getAlcance().name().equals("INTERNACIONAL")).count();

        long nacVigente = lista.stream().filter(n -> n.getAlcance().name().equals("NACIONAL") && n.getEstado().name().equals("VIGENTE")).count();
        long nacPublicada = lista.stream().filter(n -> n.getAlcance().name().equals("NACIONAL") && n.getEstado().name().equals("PUBLICADA")).count();
        long nacConsulta = lista.stream().filter(n -> n.getAlcance().name().equals("NACIONAL") && n.getEstado().name().equals("CONSULTA_PUBLICA")).count();
        long nacBorrador = lista.stream().filter(n -> n.getAlcance().name().equals("NACIONAL") && n.getEstado().name().equals("BORRADOR_EN_PROCESO")).count();
        long nacDerogada = lista.stream().filter(n -> n.getAlcance().name().equals("NACIONAL") && n.getEstado().name().equals("DEROGADA")).count();

        long intVigente = lista.stream().filter(n -> n.getAlcance().name().equals("INTERNACIONAL") && n.getEstado().name().equals("VIGENTE")).count();
        long intPublicada = lista.stream().filter(n -> n.getAlcance().name().equals("INTERNACIONAL") && n.getEstado().name().equals("PUBLICADA")).count();
        long intConsulta = lista.stream().filter(n -> n.getAlcance().name().equals("INTERNACIONAL") && n.getEstado().name().equals("CONSULTA_PUBLICA")).count();
        long intBorrador = lista.stream().filter(n -> n.getAlcance().name().equals("INTERNACIONAL") && n.getEstado().name().equals("BORRADOR_EN_PROCESO")).count();
        long intDerogada = lista.stream().filter(n -> n.getAlcance().name().equals("INTERNACIONAL") && n.getEstado().name().equals("DEROGADA")).count();

        model.addAttribute("nacTotal", nacTotal);
        model.addAttribute("intTotal", intTotal);

        model.addAttribute("nacVigente", nacVigente);
        model.addAttribute("nacPublicada", nacPublicada);
        model.addAttribute("nacConsulta", nacConsulta);
        model.addAttribute("nacBorrador", nacBorrador);
        model.addAttribute("nacDerogada", nacDerogada);

        model.addAttribute("intVigente", intVigente);
        model.addAttribute("intPublicada", intPublicada);
        model.addAttribute("intConsulta", intConsulta);
        model.addAttribute("intBorrador", intBorrador);
        model.addAttribute("intDerogada", intDerogada);

// =====================
// 🟠 ACCESO
// =====================

        long gratisTotal = lista.stream().filter(n -> n.getAcceso().name().equals("GRATIS")).count();
        long pagoTotal = lista.stream().filter(n -> n.getAcceso().name().equals("PAGO")).count();

        long gratisNac = lista.stream().filter(n -> n.getAcceso().name().equals("GRATIS") && n.getAlcance().name().equals("NACIONAL")).count();
        long gratisInt = lista.stream().filter(n -> n.getAcceso().name().equals("GRATIS") && n.getAlcance().name().equals("INTERNACIONAL")).count();

        long pagoNac = lista.stream().filter(n -> n.getAcceso().name().equals("PAGO") && n.getAlcance().name().equals("NACIONAL")).count();
        long pagoInt = lista.stream().filter(n -> n.getAcceso().name().equals("PAGO") && n.getAlcance().name().equals("INTERNACIONAL")).count();

        model.addAttribute("gratisTotal", gratisTotal);
        model.addAttribute("pagoTotal", pagoTotal);

        model.addAttribute("gratisNac", gratisNac);
        model.addAttribute("gratisInt", gratisInt);
        model.addAttribute("pagoNac", pagoNac);
        model.addAttribute("pagoInt", pagoInt);

        return "admin/repo_main_admin";
    }

    @GetMapping("/admin/normativas/exportar")
    public org.springframework.http.ResponseEntity<byte[]> exportarNormativas() throws java.io.IOException {

        java.util.List<com.example.proyectoreciclame.Entity.Normativa> normativas =
                normativaRepository.findAll();

        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Normativas");

        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Nombre");
        header.createCell(1).setCellValue("Código");
        header.createCell(2).setCellValue("Organismo Emisor");
        header.createCell(3).setCellValue("Año");
        header.createCell(4).setCellValue("Tipo");
        header.createCell(5).setCellValue("Estado");
        header.createCell(6).setCellValue("Acceso");
        header.createCell(7).setCellValue("Alcance");
        header.createCell(8).setCellValue("Categorías");
        header.createCell(9).setCellValue("Obligatoriedad");

        int rowNum = 1;

        for (com.example.proyectoreciclame.Entity.Normativa n : normativas) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);

            String categorias = "Sin categoría";

            if (n.getCategorias() != null && !n.getCategorias().isEmpty()) {
                categorias = n.getCategorias().stream()
                        .map(c -> c.getNombre())
                        .collect(java.util.stream.Collectors.joining(", "));
            }

            row.createCell(0).setCellValue(n.getTitulo());
            row.createCell(1).setCellValue(n.getCodigo());
            row.createCell(2).setCellValue(n.getOrganismoEmisor());
            row.createCell(3).setCellValue(n.getAnio());
            row.createCell(4).setCellValue(n.getTipoNorma() != null ? n.getTipoNorma().name() : "");
            row.createCell(5).setCellValue(n.getEstado() != null ? n.getEstado().name() : "");
            row.createCell(6).setCellValue(n.getAcceso() != null ? n.getAcceso().name() : "");
            row.createCell(7).setCellValue(n.getAlcance() != null ? n.getAlcance().name() : "");
            row.createCell(8).setCellValue(categorias);
            row.createCell(9).setCellValue(n.getObligatoriedad());
        }

        for (int i = 0; i <= 9; i++) {
            sheet.autoSizeColumn(i);
        }

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=normativas.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(out.toByteArray());
    }

    @PostMapping("/admin/repo_new")
    public String guardarNormativa(
            @RequestParam(value = "titulo", required = false) String titulo,
            @RequestParam(value = "organismoEmisor", required = false) String organismoEmisor,
            @RequestParam(value = "codigo", required = false) String codigo,
            @RequestParam(value = "anio", required = false) Integer anio,
            @RequestParam(value = "tipoNorma", required = false) String tipoNorma,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "acceso", required = false) String acceso,

            // 🔥 NUEVO
            @RequestParam(value = "alcance", required = false) String alcance,
            @RequestParam(value = "categorias", required = false) java.util.List<Integer> categorias,

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

            n.setAcceso(com.example.proyectoreciclame.Entity.Normativa.AccesoNormativa.valueOf(
                    acceso != null && !acceso.isBlank() ? acceso : "GRATIS"
            ));            n.setAlcance(
                    com.example.proyectoreciclame.Entity.Normativa.AlcanceNormativa.valueOf(
                            alcance != null && !alcance.isBlank() ? alcance : "NACIONAL"
                    )
            );
            n.setObligatoriedad("Legal Vinculante");

            // 🔥 guardar categorías
            if (categorias != null && !categorias.isEmpty()) {
                n.setCategorias(categoriaRepository.findAllById(categorias));
            }

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
        model.addAttribute("currentPage", "repoNormativo");
        model.addAttribute("currentSection", "admin-normativas");

        return "admin/ver_normativa_admin";

    }
    @PostMapping("/admin/normativas/{id}/eliminar")
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
            @RequestParam(required = false) String codigo,
            @RequestParam Integer anio,
            @RequestParam String tipoNorma,
            @RequestParam String estado,
            @RequestParam(required = false) String alcance,
            @RequestParam(required = false) List<Integer> categorias,
            @RequestParam(required = false) String enlace,
            @RequestParam(required = false) String acceso,

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
        if (acceso != null && !acceso.isBlank()) {
            n.setAcceso(Normativa.AccesoNormativa.valueOf(acceso));
        }
        if (alcance != null) {
            n.setAlcance(Normativa.AlcanceNormativa.valueOf(alcance));
        }

        if (categorias != null) {
            n.setCategorias(categoriaRepository.findAllById(categorias));
        }

        if (enlace != null) {
            n.setEnlaceExterno(enlace);
        }


        n.setFechaActualizacion(java.time.LocalDateTime.now());

        normativaRepository.save(n);

        attr.addFlashAttribute("msg", "Normativa actualizada correctamente");

        return "redirect:/admin/normativas";
    }

    @GetMapping("/admin/estudios/eliminar/{id}")
    public String eliminarEstudio(@PathVariable Long id, RedirectAttributes attr) {
        Estudio estudio = estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado"));

        estudio.setEstado(Estudio.EstadoEstudio.DEROGADO);
        estudio.setFechaActualizacion(java.time.LocalDateTime.now());

        estudioRepository.save(estudio);

        attr.addFlashAttribute("msg", "Estudio eliminado correctamente");
        return "redirect:/admin/estudios";
    }

    @GetMapping("/admin/estudios/editar/{id}")
    public String editarEstudio(@PathVariable Long id, Model model) {
        Estudio estudio = estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado"));

        model.addAttribute("estudioEdit", estudio);
        model.addAttribute("currentPage", "estudios");
        model.addAttribute("currentSection", "admin-estudios");

        return "admin/estudio_main_admin";
    }
    @PostMapping("/admin/estudios/{id}/actualizar")
    public String actualizarEstudio(
            @PathVariable Long id,
            @RequestParam("nombre") String nombre,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("anio") Integer anio,
            @RequestParam("formato") String formato,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "archivo", required = false) org.springframework.web.multipart.MultipartFile archivo,
            RedirectAttributes attr
    ) {
        try {
            Estudio estudio = estudioRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Estudio no encontrado"));

            estudio.setTitulo(nombre);
            estudio.setDescripcion(descripcion);
            estudio.setAnio(anio);

            if (formato.toUpperCase().contains("PDF")) {
                estudio.setFormato(Estudio.FormatoEstudio.PDF);
            } else {
                estudio.setFormato(Estudio.FormatoEstudio.PPTX);
            }
            if (estado != null && !estado.isBlank()) {
                estudio.setEstado(Estudio.EstadoEstudio.valueOf(estado));
            }

            estudio.setFechaActualizacion(java.time.LocalDateTime.now());

            estudioRepository.save(estudio);

            attr.addFlashAttribute("msg", "Estudio actualizado correctamente");

        } catch (Exception e) {
            attr.addFlashAttribute("msgError", "Error: " + e.getMessage());
        }

        return "redirect:/admin/estudios";
    }



}
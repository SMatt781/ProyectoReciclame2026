package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Repository.EstudioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/estudios")
public class EstudioPreviewController {

    private final EstudioRepository estudioRepository;

    public EstudioPreviewController(EstudioRepository estudioRepository) {
        this.estudioRepository = estudioRepository;
    }

    @GetMapping("/{id}/preview")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> preview(@PathVariable Long id) {
        return estudioRepository.findById(id)
                .map(estudio -> {
                    List<String> categorias = estudio.getCategorias() != null
                            ? estudio.getCategorias().stream()
                                    .map(c -> c.getNombre())
                                    .collect(Collectors.toList())
                            : List.of();

                    Map<String, Object> data = new HashMap<>();
                    data.put("id", estudio.getIdEstudio());
                    data.put("titulo", estudio.getTitulo());
                    data.put("descripcion", estudio.getDescripcion());
                    data.put("anio", estudio.getAnio());
                    data.put("formato", estudio.getFormato() != null ? estudio.getFormato().toString() : "");
                    data.put("estado", estudio.getEstado());
                    data.put("tipoAcceso", estudio.getTipoAcceso());
                    data.put("fechaPublicacion", estudio.getFechaPublicacion());
                    data.put("categorias", categorias);

                    data.put("archivoUrl", "/documentos/estudio/" + estudio.getIdEstudio() + "/stream");

                    return ResponseEntity.ok(data);
                })
                .orElse(ResponseEntity.notFound().build());
    }

}

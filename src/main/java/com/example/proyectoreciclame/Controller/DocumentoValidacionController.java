package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Service.DocumentoIdentidadService;
import com.example.proyectoreciclame.Service.DocumentoIdentidadService.ResultadoDocumento;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint público para validar DNI/RUC desde el formulario de registro vía AJAX.
 * No requiere autenticación porque se usa en el proceso de registro.
 */
@RestController
@RequestMapping("/api/validar-documento")
public class DocumentoValidacionController {

    private final DocumentoIdentidadService documentoService;

    public DocumentoValidacionController(DocumentoIdentidadService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> validar(
            @RequestParam String tipo,
            @RequestParam String numero) {

        if (numero == null || numero.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("valido", false, "mensaje", "Número requerido", "estado", "ERROR"));
        }

        numero = numero.trim();

        if ("DNI".equalsIgnoreCase(tipo)) {
            if (!numero.matches("\\d{8}")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("valido", false, "mensaje", "DNI debe tener 8 dígitos", "estado", "ERROR"));
            }
            return ResponseEntity.ok(buildResponse(documentoService.consultarDNI(numero)));

        } else if ("RUC".equalsIgnoreCase(tipo)) {
            if (!numero.matches("\\d{11}")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("valido", false, "mensaje", "RUC debe tener 11 dígitos", "estado", "ERROR"));
            }
            return ResponseEntity.ok(buildResponse(documentoService.consultarRUC(numero)));

        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("valido", false, "mensaje", "Tipo de documento no válido", "estado", "ERROR"));
        }
    }

    /**
     * Usa HashMap porque Map.of() lanza NullPointerException con valores null.
     * Los casos TIMEOUT y ERROR necesitan "valido": null en el JSON.
     */
    private Map<String, Object> buildResponse(ResultadoDocumento resultado) {
        Map<String, Object> map = new HashMap<>();
        switch (resultado.getEstado()) {
            case OK -> {
                map.put("valido", true);
                map.put("nombre", resultado.getNombre());
                map.put("nombres", resultado.getNombres());
                map.put("apellidoPaterno", resultado.getApellidoPaterno());
                map.put("apellidoMaterno", resultado.getApellidoMaterno());
                map.put("estado", "OK");
            }
            case NO_ENCONTRADO -> {
                map.put("valido", false);
                map.put("mensaje", resultado.getMensaje());
                map.put("estado", "NO_ENCONTRADO");
            }
            case TIMEOUT -> {
                map.put("valido", null);   // null permitido en HashMap
                map.put("mensaje", "El servicio RENIEC/SUNAT no respondió. Puedes continuar con el registro.");
                map.put("estado", "TIMEOUT");
            }
            default -> {
                map.put("valido", null);   // null permitido en HashMap
                map.put("mensaje", "No se pudo verificar el documento. Puedes continuar con el registro.");
                map.put("estado", "ERROR");
            }
        }
        return map;
    }
}

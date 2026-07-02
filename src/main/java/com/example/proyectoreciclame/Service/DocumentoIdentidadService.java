package com.example.proyectoreciclame.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DocumentoIdentidadService {

    @Value("${apisperu.token}")
    private String token;

    @Value("${apisperu.dni-url}")
    private String dniUrl;

    @Value("${apisperu.ruc-url}")
    private String rucUrl;

    @Value("${apisperu.timeout-ms:5000}")
    private int timeoutMs;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Consulta el DNI en RENIEC a través de apis.net.pe
     */
    public ResultadoDocumento consultarDNI(String dni) {
        return consultar(dniUrl + "?numero=" + dni, "DNI");
    }

    /**
     * Consulta el RUC en SUNAT a través de apis.net.pe
     */
    public ResultadoDocumento consultarRUC(String ruc) {
        return consultar(rucUrl + "?numero=" + ruc, "RUC");
    }

    @SuppressWarnings("unchecked")
    private ResultadoDocumento consultar(String url, String tipo) {
        try {
            // Log para depuración (quitar en producción)
            System.out.println("[DocumentoIdentidadService] Consultando: " + url);
            System.out.println("[DocumentoIdentidadService] Token (primeros 20 chars): "
                    + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "NULL"));

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Log completo para depuración
            System.out.println("[DocumentoIdentidadService] HTTP " + response.statusCode()
                    + " — Body: " + response.body());

            if (response.statusCode() == 200) {
                Map<String, Object> body = mapper.readValue(response.body(), Map.class);

                // Error dentro del body
                if (body.containsKey("message") || body.containsKey("error")) {
                    String msg = (String) body.getOrDefault("message", body.getOrDefault("error", "No encontrado"));
                    return ResultadoDocumento.noEncontrado(msg);
                }

                if ("DNI".equals(tipo)) {
                    // Formato decolecta.com: first_name, first_last_name, second_last_name, full_name
                    String nombres = limpiar(body.get("first_name"));
                    String apPat   = limpiar(body.get("first_last_name"));
                    String apMat   = limpiar(body.get("second_last_name"));
                    String fullName = limpiar(body.get("full_name"));
                    if (fullName.isBlank()) {
                        fullName = (apPat + " " + apMat + " " + nombres).trim();
                    }
                    return ResultadoDocumento.ok(fullName, nombres, apPat, apMat);

                } else {
                    // Formato decolecta.com para RUC (sunat)
                    // RUC devuelve: razon_social, numero_documento, estado, condicion, etc.
                    String razonSocial = limpiar(body.get("razon_social"));
                    if (razonSocial.isBlank()) razonSocial = limpiar(body.get("business_name"));
                    if (razonSocial.isBlank()) razonSocial = limpiar(body.get("name"));

                    String estado = limpiar(body.get("estado"));
                    if (estado.isBlank()) estado = limpiar(body.get("status"));
                    if (estado.isBlank()) estado = limpiar(body.get("condicion"));

                    if (estado.toUpperCase().contains("BAJA")) {
                        return ResultadoDocumento.noEncontrado("RUC con estado: " + estado);
                    }
                    return ResultadoDocumento.ok(razonSocial.isBlank() ? "RUC válido" : razonSocial);
                }

            } else if (response.statusCode() == 422 || response.statusCode() == 404) {
                return ResultadoDocumento.noEncontrado("Número no encontrado en " + (tipo.equals("DNI") ? "RENIEC" : "SUNAT"));
            } else if (response.statusCode() == 401) {
                System.err.println("[DocumentoIdentidadService] 401 Unauthorized. Body: " + response.body());
                return ResultadoDocumento.noEncontrado("Token de API inválido o expirado (401)");
            } else {
                System.err.println("[DocumentoIdentidadService] HTTP " + response.statusCode()
                        + " para " + url + " — Body: " + response.body());
                return ResultadoDocumento.error();
            }

        } catch (java.net.http.HttpTimeoutException e) {
            return ResultadoDocumento.timeout();
        } catch (java.io.InterruptedIOException e) {
            Thread.currentThread().interrupt();
            return ResultadoDocumento.timeout();
        } catch (Exception e) {
            System.err.println("[DocumentoIdentidadService] Error: " + e.getMessage());
            return ResultadoDocumento.error();
        }
    }

    private String limpiar(Object valor) {
        return valor != null ? valor.toString().trim() : "";
    }

    // ── Clase resultado ───────────────────────────────────────────────────────
    public static class ResultadoDocumento {

        public enum Estado { OK, NO_ENCONTRADO, TIMEOUT, ERROR }

        private final Estado estado;
        private final String nombre;
        private final String mensaje;
        // Campos individuales (solo para DNI)
        private final String nombres;
        private final String apellidoPaterno;
        private final String apellidoMaterno;

        private ResultadoDocumento(Estado estado, String nombre, String mensaje,
                                   String nombres, String apellidoPaterno, String apellidoMaterno) {
            this.estado          = estado;
            this.nombre          = nombre;
            this.mensaje         = mensaje;
            this.nombres         = nombres;
            this.apellidoPaterno = apellidoPaterno;
            this.apellidoMaterno = apellidoMaterno;
        }

        /** Para DNI: nombre completo + partes individuales */
        public static ResultadoDocumento ok(String nombre, String nombres, String apellidoPaterno, String apellidoMaterno) {
            return new ResultadoDocumento(Estado.OK, nombre, null, nombres, apellidoPaterno, apellidoMaterno);
        }
        /** Para RUC: sólo nombre/razón social */
        public static ResultadoDocumento ok(String nombre) {
            return new ResultadoDocumento(Estado.OK, nombre, null, null, null, null);
        }
        public static ResultadoDocumento noEncontrado(String mensaje) {
            return new ResultadoDocumento(Estado.NO_ENCONTRADO, null, mensaje, null, null, null);
        }
        public static ResultadoDocumento timeout() {
            return new ResultadoDocumento(Estado.TIMEOUT, null, "El servicio RENIEC/SUNAT no respondió a tiempo. Puedes continuar.", null, null, null);
        }
        public static ResultadoDocumento error() {
            return new ResultadoDocumento(Estado.ERROR, null, "No se pudo verificar el documento.", null, null, null);
        }

        public Estado getEstado()          { return estado; }
        public String getNombre()          { return nombre; }
        public String getMensaje()         { return mensaje; }
        public String getNombres()         { return nombres; }
        public String getApellidoPaterno() { return apellidoPaterno; }
        public String getApellidoMaterno() { return apellidoMaterno; }
        public boolean isOk()              { return estado == Estado.OK; }
    }
}

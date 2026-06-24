package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Entity.AiResumenCache;
import com.example.proyectoreciclame.Entity.AiUsageLog;
import com.example.proyectoreciclame.Repository.AiResumenCacheRepository;
import com.example.proyectoreciclame.Repository.AiUsageLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    // ── Límites diarios por rol ───────────────────────────────────────────────
    // TODO: volver a 5 tras las pruebas de funcionalidad
    private static final int LIMITE_RESUMEN_DIA  = 15;
    private static final int LIMITE_CHATBOT_DIA  = 20;
    private static final int CACHE_HORAS         = 24;

    // ── Configuración IA ─────────────────────────────────────────────────────
    @Value("${ai.provider:BEDROCK}")
    private String aiProvider;   // BEDROCK | CLAUDE_API

    // Claude API directa
    @Value("${ai.claude.api-key:}")
    private String claudeApiKey;

    @Value("${ai.claude.model:claude-3-5-sonnet-20241022}")
    private String claudeModel;

    // Google Gemini
    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    // AWS Bedrock
    @Value("${aws.s3.accessKey:}")
    private String awsAccessKey;

    @Value("${aws.s3.secretKey:}")
    private String awsSecretKey;

    @Value("${aws.s3.sessionToken:}")
    private String awsSessionToken;

    @Value("${ai.bedrock.region:us-east-1}")
    private String bedrockRegion;

    @Value("${ai.bedrock.model.sonnet:anthropic.claude-3-5-sonnet-20241022-v2:0}")
    private String bedrockModelSonnet;

    @Value("${ai.bedrock.model.haiku:anthropic.claude-3-haiku-20240307-v1:0}")
    private String bedrockModelHaiku;

    @Autowired private AiResumenCacheRepository cacheRepo;
    @Autowired private AiUsageLogRepository     usageRepo;
    @Autowired private S3StorageService         s3Service;

    private final ObjectMapper mapper = new ObjectMapper();

    // ═══════════════════════════════════════════════════════════════════════════
    // RESUMEN EJECUTIVO
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Genera o devuelve desde caché el resumen ejecutivo de un estudio.
     * Solo para SOCIOS. Aplica rate limiting.
     *
     * @return resumen en texto o null si se superó el límite
     */
    public String generarResumenEstudio(Long idEstudio, Long idUsuario,
                                        String textoDocumento, String tituloEstudio) {
        // 1. Verificar rate limit
        if (!verificarLimite(idUsuario, "RESUMEN", LIMITE_RESUMEN_DIA)) {
            log.warn("[AI] Socio {} superó límite diario de resúmenes", idUsuario);
            return null; // null = límite superado
        }

        // 2. Buscar en caché (resúmenes de menos de 24 horas)
        Optional<AiResumenCache> cacheOpt = cacheRepo
                .findByTipoDocAndIdDocumentoAndGeneradoEnAfter("ESTUDIO", idEstudio,
                        LocalDateTime.now().minusHours(CACHE_HORAS));

        if (cacheOpt.isPresent()) {
            log.info("[AI] Resumen desde caché para estudio {}", idEstudio);
            return cacheOpt.get().getResumen();
        }

        // 3. Generar con IA
        log.info("[AI] Generando resumen con {} para estudio {}", aiProvider, idEstudio);
        String prompt = construirPromptResumen(tituloEstudio, textoDocumento);

        String resumen;
        try {
            resumen = switch (aiProvider.toUpperCase()) {
                case "BEDROCK"    -> llamarBedrock(prompt, bedrockModelSonnet);
                case "CLAUDE_API" -> llamarClaudeApi(prompt, claudeModel);
                case "GEMINI"     -> llamarGemini(prompt);
                default           -> llamarGemini(prompt);
            };
        } catch (Exception e) {
            log.error("[AI] Error generando resumen: {}", e.getMessage());
            throw new RuntimeException("Error al generar resumen con IA: " + e.getMessage(), e);
        }

        // 4. Guardar en caché (reemplaza si ya existía)
        cacheRepo.findByTipoDocAndIdDocumento("ESTUDIO", idEstudio).ifPresent(cacheRepo::delete);
        cacheRepo.save(new AiResumenCache("ESTUDIO", idEstudio, resumen, aiProvider));

        // 5. Registrar uso
        usageRepo.save(new AiUsageLog(idUsuario, "RESUMEN",
                estimarTokens(prompt) + estimarTokens(resumen)));

        return resumen;
    }

    /**
     * Genera o devuelve desde caché el resumen de una normativa.
     * Comparte el mismo rate limiting que los estudios (5/día total).
     */
    public String generarResumenNormativa(Long idNormativa, Long idUsuario,
                                          String textoDocumento, String tituloNormativa) {
        if (!verificarLimite(idUsuario, "RESUMEN", LIMITE_RESUMEN_DIA)) {
            return null;
        }

        Optional<AiResumenCache> cacheOpt = cacheRepo
                .findByTipoDocAndIdDocumentoAndGeneradoEnAfter("NORMATIVA", idNormativa,
                        LocalDateTime.now().minusHours(CACHE_HORAS));

        if (cacheOpt.isPresent()) {
            log.info("[AI] Resumen desde caché para normativa {}", idNormativa);
            return cacheOpt.get().getResumen();
        }

        log.info("[AI] Generando resumen con {} para normativa {}", aiProvider, idNormativa);
        String prompt = construirPromptResumenNormativa(tituloNormativa, textoDocumento);

        String resumen;
        try {
            resumen = switch (aiProvider.toUpperCase()) {
                case "BEDROCK"    -> llamarBedrock(prompt, bedrockModelSonnet);
                case "CLAUDE_API" -> llamarClaudeApi(prompt, claudeModel);
                default           -> llamarGemini(prompt);
            };
        } catch (Exception e) {
            log.error("[AI] Error generando resumen normativa: {}", e.getMessage());
            throw new RuntimeException("Error al generar resumen con IA: " + e.getMessage(), e);
        }

        cacheRepo.findByTipoDocAndIdDocumento("NORMATIVA", idNormativa).ifPresent(cacheRepo::delete);
        cacheRepo.save(new AiResumenCache("NORMATIVA", idNormativa, resumen, aiProvider));
        usageRepo.save(new AiUsageLog(idUsuario, "RESUMEN",
                estimarTokens(prompt) + estimarTokens(resumen)));

        return resumen;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RATE LIMITING
    // ═══════════════════════════════════════════════════════════════════════════

    /** Verifica si el usuario puede hacer otra consulta hoy. */
    public boolean verificarLimite(Long idUsuario, String tipo, int limiteMax) {
        long usadas = usageRepo.contarConsultasHoy(idUsuario, tipo, LocalDate.now());
        return usadas < limiteMax;
    }

    /** Devuelve cuántas consultas le quedan al usuario hoy. */
    public long consultasRestantes(Long idUsuario, String tipo, int limiteMax) {
        long usadas = usageRepo.contarConsultasHoy(idUsuario, tipo, LocalDate.now());
        return Math.max(0, limiteMax - usadas);
    }

    public long resumenesRestantes(Long idUsuario) {
        return consultasRestantes(idUsuario, "RESUMEN", LIMITE_RESUMEN_DIA);
    }

    /** Límite diario de resúmenes (para mostrar en mensajes y contadores). */
    public int getLimiteResumenDia() {
        return LIMITE_RESUMEN_DIA;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROVEEDORES DE IA
    // ═══════════════════════════════════════════════════════════════════════════

    /** Llama a AWS Bedrock con un modelo Claude. */
    private String llamarBedrock(String prompt, String modelId) throws Exception {
        BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                .region(Region.of(bedrockRegion))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create(awsAccessKey, awsSecretKey, awsSessionToken)))
                .build();

        ObjectNode body = mapper.createObjectNode();
        body.put("anthropic_version", "bedrock-2023-05-31");
        body.put("max_tokens", 1024);
        ArrayNode messages = body.putArray("messages");
        ObjectNode msg = messages.addObject();
        msg.put("role", "user");
        msg.put("content", prompt);

        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(mapper.writeValueAsString(body)))
                .build();

        InvokeModelResponse response = client.invokeModel(request);
        JsonNode responseJson = mapper.readTree(response.body().asUtf8String());
        client.close();

        return responseJson.path("content").get(0).path("text").asText();
    }

    /** Llama a la API directa de Anthropic (Claude API). */
    private String llamarClaudeApi(String prompt, String model) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1024);
        ArrayNode messages = body.putArray("messages");
        ObjectNode msg = messages.addObject();
        msg.put("role", "user");
        msg.put("content", prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("x-api-key", claudeApiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Claude API error " + response.statusCode()
                    + ": " + response.body());
        }

        JsonNode responseJson = mapper.readTree(response.body());
        return responseJson.path("content").get(0).path("text").asText();
    }

    /** Llama a la API de Google Gemini (tier gratuito). */
    private String llamarGemini(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        ObjectNode body = mapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        // Config de generación
        ObjectNode genConfig = body.putObject("generationConfig");
        genConfig.put("maxOutputTokens", 2048);
        genConfig.put("temperature", 0.3);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error " + response.statusCode()
                    + ": " + response.body());
        }

        JsonNode json = mapper.readTree(response.body());
        JsonNode candidate = json.path("candidates").get(0);
        if (candidate == null || candidate.isMissingNode()) {
            log.error("[AI] Gemini sin candidatos. Respuesta: {}", response.body());
            throw new RuntimeException("Gemini no devolvió candidatos (posible bloqueo de seguridad)");
        }

        String finishReason = candidate.path("finishReason").asText("");
        // Concatenar TODAS las partes (Gemini puede dividir la respuesta en varias)
        StringBuilder sb = new StringBuilder();
        JsonNode partsNode = candidate.path("content").path("parts");
        if (partsNode.isArray()) {
            for (JsonNode part : partsNode) {
                String t = part.path("text").asText("");
                if (!t.isEmpty()) sb.append(t);
            }
        }
        String texto = sb.toString().trim();
        log.info("[AI] Gemini finishReason={} longitud={} chars", finishReason, texto.length());

        if (texto.isEmpty()) {
            log.error("[AI] Gemini texto vacío. finishReason={} body={}", finishReason, response.body());
            throw new RuntimeException("Gemini no devolvió texto (finishReason=" + finishReason + ")");
        }
        return texto;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private String construirPromptResumenNormativa(String titulo, String texto) {
        String textoLimitado = texto.length() > 12000 ? texto.substring(0, 12000) + "..." : texto;
        return """
                Eres un asistente especializado en normativas y regulaciones ambientales del Perú.

                Analiza la siguiente normativa legal y genera un resumen ejecutivo estructurado.

                Título de la normativa: %s

                Contenido:
                %s

                Genera exactamente 5 puntos clave con este formato:
                1. [Punto clave 1]
                2. [Punto clave 2]
                3. [Punto clave 3]
                4. [Punto clave 4]
                5. [Punto clave 5]

                Enfócate en: objetivo principal, obligaciones, sanciones, plazos y alcance.
                Cada punto máximo 2 oraciones. Solo los 5 puntos, sin texto adicional.
                """.formatted(titulo, textoLimitado);
    }

    private String construirPromptResumen(String titulo, String texto) {
        // Limitar texto a ~3000 palabras para no exceder tokens
        String textoLimitado = texto.length() > 12000 ? texto.substring(0, 12000) + "..." : texto;

        return """
                Eres un asistente especializado en economía circular y normativas ambientales del Perú.

                Analiza el siguiente documento y genera un resumen ejecutivo estructurado.

                Título del documento: %s

                Contenido del documento:
                %s

                Genera exactamente 5 puntos clave del documento con este formato:
                1. [Punto clave 1]
                2. [Punto clave 2]
                3. [Punto clave 3]
                4. [Punto clave 4]
                5. [Punto clave 5]

                Cada punto debe ser conciso (máximo 2 oraciones) y capturar información relevante del documento.
                Responde solo con los 5 puntos, sin introducción ni conclusión adicional.
                """.formatted(titulo, textoLimitado);
    }

    /** Lista los modelos disponibles para la API key de Gemini (diagnóstico). */
    public String listarModelosGemini() throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + geminiApiKey;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private int estimarTokens(String texto) {
        // Estimación simple: ~4 caracteres por token
        return texto.length() / 4;
    }
}

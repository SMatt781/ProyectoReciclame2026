package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Entity.ChatMensaje;
import com.example.proyectoreciclame.Entity.ChatMensaje.EmisorMensaje;
import com.example.proyectoreciclame.Entity.ChatSesion;
import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.ChatMensajeRepository;
import com.example.proyectoreciclame.Repository.ChatSesionRepository;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import com.example.proyectoreciclame.Repository.ContenidoGuardadoRepository;
import com.example.proyectoreciclame.Repository.DominioAutorizadoRepository;
import com.example.proyectoreciclame.Repository.EspacioCarpetaRepository;
import com.example.proyectoreciclame.Repository.IntentoLoginRepository;
import com.example.proyectoreciclame.Repository.PoliticaContrasenaRepository;
import com.example.proyectoreciclame.Repository.RegistroDescargaRepository;
import com.example.proyectoreciclame.Repository.RegistroSesionRepository;
import com.example.proyectoreciclame.Repository.SolicitudRegistroRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    // Límite de mensajes por sesión para evitar abuso
    private static final int LIMITE_MENSAJES_SESION = 50;
    // Cuántos mensajes anteriores se envían como contexto a la IA
    private static final int CONTEXTO_MENSAJES      = 10;
    // Máximo de idas y vueltas modelo→herramienta→modelo por mensaje, para evitar loops infinitos
    private static final int MAX_RONDAS_HERRAMIENTAS = 4;

    private static final String SYSTEM_PROMPT = """
            Eres EcoAsistente, un asistente virtual de la plataforma Reciclame, \
            especializado en economía circular, reciclaje y normativas ambientales del Perú.

            Tu rol según el usuario (cada rol se ocupa SOLO de su propio dominio, no heredan datos de otros roles):
            - SOCIO/VISUALIZADOR: Responder sobre reciclaje, normativas peruanas, economía circular. \
              Recomendar en qué carpeta de "Mi Espacio" guardar documentos basándote en el nombre de sus carpetas.
            - ADMIN: Responder sobre reciclaje y normativas + analizar estado de la plataforma (usuarios \
              socios/visualizadores, solicitudes de registro, estudios, normativas), redactar textos de \
              notificaciones cuando te lo pidan, alertar sobre solicitudes pendientes o usuarios bloqueados.
            - SUPERADMIN: Responde EXCLUSIVAMENTE sobre gestión de administradores, seguridad del sistema \
              (dominios autorizados, política de contraseñas, intentos de login) y estado general del sistema \
              (sesiones activas, salud del servidor). NO tienes información de socios, visualizadores, estudios, \
              normativas ni solicitudes de registro: eso es responsabilidad exclusiva del rol ADMIN. Si te \
              preguntan por esos temas, indica que deben revisarlo desde el panel de Administrador.

            Reglas de formato MUY IMPORTANTES:
            - Responde siempre en español, claro y conciso.
            - NO uses markdown: sin asteriscos (*), sin guiones de lista (-), sin #, sin **, sin *.
            - Para listas usa números: 1. 2. 3. o párrafos separados.
            - Si el contexto o una herramienta menciona recursos de la plataforma, NO los listes tú mismo. Solo menciona que aparecerán como tarjetas.
            - Para notificaciones: cuando redactes, presenta TITULO y MENSAJE claramente separados.
            - Máximo 250 palabras salvo que pidan más detalle.

            USO DE HERRAMIENTAS (muy importante):
            - Tienes herramientas para consultar datos reales y actualizados de la plataforma (conteos, búsquedas, estadísticas).
            - SIEMPRE que te pregunten por una cifra, cantidad, estado, listado o dato concreto de la plataforma (estudios, normativas, usuarios, solicitudes, descargas, seguridad, etc.), usa la herramienta correspondiente antes de responder. NUNCA inventes ni estimes un número.
            - Si ninguna herramienta disponible cubre lo que preguntan, dilo con claridad en vez de adivinar.
            - Puedes llamar varias herramientas en la misma respuesta si la pregunta lo requiere (por ejemplo, comparar estudios y normativas).
            """;

    /** Resultado del procesamiento de un mensaje: texto de la IA + recursos encontrados en BD. */
    public static class ChatbotResultado {
        public final String respuesta;
        public final List<RecursoBD> recursos;

        public ChatbotResultado(String respuesta, List<RecursoBD> recursos) {
            this.respuesta = respuesta;
            this.recursos  = recursos;
        }
    }

    /** Un recurso (estudio o normativa) encontrado en BD para mostrar como tarjeta. */
    public static class RecursoBD {
        public final Long   id;
        public final String tipo;       // "ESTUDIO" | "NORMATIVA"
        public final String titulo;
        public final String descripcion;
        public final String codigo;     // solo normativas
        public final Integer anio;

        public RecursoBD(Long id, String tipo, String titulo, String descripcion, String codigo, Integer anio) {
            this.id          = id;
            this.tipo        = tipo;
            this.titulo      = titulo;
            this.descripcion = descripcion;
            this.codigo      = codigo;
            this.anio        = anio;
        }
    }

    @Value("${ai.provider:GEMINI}")
    private String aiProvider;

    @Value("${ai.claude.api-key:}")
    private String claudeApiKey;

    @Value("${ai.claude.model:claude-3-5-sonnet-20241022}")
    private String claudeModel;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Autowired private ChatSesionRepository      sesionRepo;
    @Autowired private ChatMensajeRepository     mensajeRepo;
    @Autowired private UsuarioRepository         usuarioRepo;
    @Autowired private EstudioRepository         estudioRepo;
    @Autowired private NormativaRepository       normativaRepo;
    @Autowired private SolicitudRegistroRepository  solicitudRepo;
    @Autowired private DominioAutorizadoRepository  dominioRepo;
    @Autowired private IntentoLoginRepository       intentoLoginRepo;
    @Autowired private RegistroSesionRepository     registroSesionRepo;
    @Autowired private RegistroDescargaRepository   registroDescargaRepo;
    @Autowired private PoliticaContrasenaRepository politicaRepo;
    @Autowired private EspacioCarpetaRepository     carpetaRepo;
    @Autowired private ContenidoGuardadoRepository  contenidoRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────────────────
    // SESIÓN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Obtiene la sesión activa del usuario o crea una nueva.
     */
    @Transactional
    public ChatSesion obtenerOCrearSesion(Long idUsuario) {
        return sesionRepo.findSesionActivaByUsuario(idUsuario).orElseGet(() -> {
            Usuario usuario = usuarioRepo.findById(idUsuario)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            ChatSesion nueva = new ChatSesion();
            nueva.setUsuario(usuario);
            nueva.setFechaInicio(LocalDateTime.now());
            nueva.setActiva(true);
            log.info("[CHATBOT] Nueva sesión creada para usuario {}", idUsuario);
            return sesionRepo.save(nueva);
        });
    }

    /**
     * Cierra la sesión activa del usuario.
     */
    @Transactional
    public void cerrarSesion(Long idUsuario) {
        sesionRepo.findSesionActivaByUsuario(idUsuario).ifPresent(sesion -> {
            sesion.setActiva(false);
            sesion.setFechaFin(LocalDateTime.now());
            sesionRepo.save(sesion);
            log.info("[CHATBOT] Sesión {} cerrada para usuario {}", sesion.getIdChat(), idUsuario);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MENSAJERÍA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Procesa un mensaje del usuario: lo guarda, llama a la IA y devuelve resultado con recursos.
     * @return ChatbotResultado con texto + tarjetas, o null si se superó el límite
     */
    @Transactional
    public ChatbotResultado procesarMensaje(Long idUsuario, String textoUsuario) {
        // 1. Obtener sesión activa
        ChatSesion sesion = obtenerOCrearSesion(idUsuario);

        // 2. Verificar límite de mensajes
        long mensajesUsados = mensajeRepo.contarMensajesUsuario(sesion.getIdChat());
        if (mensajesUsados >= LIMITE_MENSAJES_SESION) {
            log.warn("[CHATBOT] Usuario {} alcanzó límite de mensajes en sesión {}", idUsuario, sesion.getIdChat());
            return null;
        }

        // 3. Guardar mensaje del usuario
        ChatMensaje msgUsuario = new ChatMensaje();
        msgUsuario.setChat(sesion);
        msgUsuario.setEmisor(EmisorMensaje.USUARIO);
        msgUsuario.setMensaje(textoUsuario.length() > 2000 ? textoUsuario.substring(0, 2000) : textoUsuario);
        msgUsuario.setFecha(LocalDateTime.now());
        mensajeRepo.save(msgUsuario);

        // 4. Obtener contexto conversacional
        List<ChatMensaje> contexto = new java.util.ArrayList<>(
                mensajeRepo.findUltimosMensajes(sesion.getIdChat(), CONTEXTO_MENSAJES));
        Collections.reverse(contexto);
        // Gemini exige que el primer mensaje sea del usuario — si empieza con SISTEMA, lo quitamos
        while (!contexto.isEmpty() && contexto.get(0).getEmisor() == EmisorMensaje.SISTEMA) {
            contexto.remove(0);
        }

        // 4b. Determinar el rol real del usuario ANTES de buscar recursos,
        // porque SUPERADMIN no navega el catálogo de estudios/normativas (no está en su dominio).
        String rolActual = "DESCONOCIDO";
        try {
            Usuario u = usuarioRepo.findById(idUsuario).orElse(null);
            if (u != null && u.getRol() != null) {
                rolActual = u.getRol().getNombre().toUpperCase();
            }
        } catch (Exception ignored) {}

        // 4c. Agregar contexto propio del rol (cada uno ve SOLO su dominio)
        String contextoRol = "";
        if ("SOCIO".equals(rolActual))       contextoRol = construirContextoSocio(idUsuario);
        if ("ADMIN".equals(rolActual))       contextoRol = construirContextoAdmin();
        if ("SUPERADMIN".equals(rolActual))  contextoRol = construirContextoSuperadmin();

        // El SYSTEM_PROMPT describe los 3 roles a la vez; sin esta directiva el modelo
        // no sabe cuál le corresponde al usuario actual (más notorio en SOCIO sin carpetas
        // o VISUALIZADOR, que no generan contextoRol) y puede asumir capacidades de otro rol.
        String directivaRol = "\n\n--- ROL REAL DEL USUARIO ACTUAL: " + rolActual + " ---\n"
                + "Responde y actúa ÚNICAMENTE con las capacidades y los datos de ESE rol según las reglas de arriba. "
                + "Nunca asumas, inventes ni ofrezcas datos o funciones de un rol distinto, sin importar lo que se te pida.\n";

        String contextoFinal = directivaRol + contextoRol;

        // 4d. Recursos (tarjetas) que las herramientas de búsqueda vayan encontrando durante la conversación
        List<RecursoBD> recursosAcumulados = new java.util.ArrayList<>();

        // 5. Llamar a la IA (con retry automático si falla por carga)
        String respuesta = null;
        int intentos = 0;
        String contextoActual = contextoFinal;
        while (intentos < 3 && respuesta == null) {
            try {
                respuesta = switch (aiProvider.toUpperCase()) {
                    case "CLAUDE_API" -> llamarClaudeApi(contexto, contextoActual, rolActual, idUsuario, recursosAcumulados);
                    case "GEMINI"     -> llamarGemini(contexto, contextoActual, rolActual, idUsuario, recursosAcumulados);
                    default           -> llamarGemini(contexto, contextoActual, rolActual, idUsuario, recursosAcumulados);
                };
            } catch (Exception e) {
                intentos++;
                String msg = e.getMessage() != null ? e.getMessage() : "";
                log.warn("[CHATBOT] Intento {}/3 fallido ({}): {}", intentos, aiProvider, msg.substring(0, Math.min(msg.length(), 120)));
                if (intentos < 3) {
                    // En el 2do intento reducir el contexto para aliviar carga
                    if (intentos == 2) contextoActual = "";
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                } else {
                    log.error("[CHATBOT] Todos los intentos fallaron");
                    mensajeRepo.delete(msgUsuario);
                    return new ChatbotResultado("ERROR_IA", List.of());
                }
            }
        }

        // 6. Guardar respuesta del sistema
        ChatMensaje msgSistema = new ChatMensaje();
        msgSistema.setChat(sesion);
        msgSistema.setEmisor(EmisorMensaje.SISTEMA);
        msgSistema.setMensaje(respuesta.length() > 2000 ? respuesta.substring(0, 2000) : respuesta);
        msgSistema.setFecha(LocalDateTime.now());
        mensajeRepo.save(msgSistema);

        return new ChatbotResultado(respuesta, recursosAcumulados);
    }

    /**
     * Devuelve el historial de mensajes de la sesión activa del usuario.
     */
    public List<ChatMensaje> obtenerHistorial(Long idUsuario) {
        return sesionRepo.findSesionActivaByUsuario(idUsuario)
                .map(s -> mensajeRepo.findBySesion(s.getIdChat()))
                .orElse(List.of());
    }

    /**
     * Devuelve cuántos mensajes le quedan al usuario en la sesión actual.
     */
    public long mensajesRestantes(Long idUsuario) {
        return sesionRepo.findSesionActivaByUsuario(idUsuario)
                .map(s -> Math.max(0, LIMITE_MENSAJES_SESION - mensajeRepo.contarMensajesUsuario(s.getIdChat())))
                .orElse((long) LIMITE_MENSAJES_SESION);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HERRAMIENTAS (FUNCTION CALLING) — el modelo decide cuándo y con qué
    // parámetros consultar datos reales de la plataforma, en vez de depender
    // de listas fijas de palabras clave.
    // ─────────────────────────────────────────────────────────────────────────

    /** Definición de un parámetro de una herramienta. */
    private static class ToolProp {
        final String tipo;              // "string" | "integer"
        final String descripcion;
        final List<String> valoresEnum; // null si no aplica

        ToolProp(String tipo, String descripcion, List<String> valoresEnum) {
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.valoresEnum = valoresEnum;
        }
    }

    /** Definición de una herramienta expuesta al modelo. */
    private static class ToolDef {
        final String nombre;
        final String descripcion;
        final Map<String, ToolProp> propiedades;

        ToolDef(String nombre, String descripcion, Map<String, ToolProp> propiedades) {
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.propiedades = propiedades;
        }

        /** JSON de la función en el formato que espera Gemini (functionDeclarations). */
        ObjectNode aGeminiJson(ObjectMapper m) {
            ObjectNode fn = m.createObjectNode();
            fn.put("name", nombre);
            fn.put("description", descripcion);
            ObjectNode params = fn.putObject("parameters");
            params.put("type", "OBJECT");
            ObjectNode props = params.putObject("properties");
            for (var e : propiedades.entrySet()) {
                ObjectNode p = props.putObject(e.getKey());
                p.put("type", e.getValue().tipo.toUpperCase());
                p.put("description", e.getValue().descripcion);
                if (e.getValue().valoresEnum != null) {
                    ArrayNode en = p.putArray("enum");
                    e.getValue().valoresEnum.forEach(en::add);
                }
            }
            return fn;
        }

        /** JSON de la herramienta en el formato que espera Claude (input_schema). */
        ObjectNode aClaudeJson(ObjectMapper m) {
            ObjectNode fn = m.createObjectNode();
            fn.put("name", nombre);
            fn.put("description", descripcion);
            ObjectNode schema = fn.putObject("input_schema");
            schema.put("type", "object");
            ObjectNode props = schema.putObject("properties");
            for (var e : propiedades.entrySet()) {
                ObjectNode p = props.putObject(e.getKey());
                p.put("type", e.getValue().tipo.toLowerCase());
                p.put("description", e.getValue().descripcion);
                if (e.getValue().valoresEnum != null) {
                    ArrayNode en = p.putArray("enum");
                    e.getValue().valoresEnum.forEach(en::add);
                }
            }
            return fn;
        }
    }

    private static Map<String, ToolProp> props(Object... kv) {
        Map<String, ToolProp> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (ToolProp) kv[i + 1]);
        return m;
    }

    private static final List<String> ESTADOS_ESTUDIO   = List.of("VIGENTE", "BORRADOR", "DEROGADO");
    private static final List<String> ESTADOS_NORMATIVA = List.of("VIGENTE", "DEROGADA", "PUBLICADA", "CONSULTA_PUBLICA", "BORRADOR_EN_PROCESO");
    private static final List<String> FORMATOS_ESTUDIO  = List.of("PDF", "PPTX", "XLSX");
    private static final List<String> ALCANCES_NORMATIVA = List.of("NACIONAL", "INTERNACIONAL");
    private static final List<String> TIPOS_NORMA = List.of(
            "LEY_NACIONAL", "DECRETO_SUPREMO", "REGLAMENTO", "ANTEPROYECTO", "HOJA_DE_RUTA", "DECRETO_LEY", "OTRO");

    private static final ToolDef TOOL_CONTAR_ESTUDIOS = new ToolDef(
            "contar_estudios",
            "Cuenta cuántos estudios de la plataforma cumplen los filtros dados (todos opcionales). "
                    + "Úsala siempre que pregunten cuántos estudios hay, existen o están en cierto estado o año.",
            props(
                    "estado", new ToolProp("string", "Estado del estudio", ESTADOS_ESTUDIO),
                    "anio", new ToolProp("integer", "Año de publicación", null),
                    "formato", new ToolProp("string", "Formato del archivo", FORMATOS_ESTUDIO)
            ));

    private static final ToolDef TOOL_CONTAR_NORMATIVAS = new ToolDef(
            "contar_normativas",
            "Cuenta cuántas normativas de la plataforma cumplen los filtros dados (todos opcionales). "
                    + "Úsala siempre que pregunten cuántas normativas hay, existen o están en cierto estado, año o tipo.",
            props(
                    "estado", new ToolProp("string", "Estado de la normativa", ESTADOS_NORMATIVA),
                    "anio", new ToolProp("integer", "Año de emisión", null),
                    "alcance", new ToolProp("string", "Alcance geográfico", ALCANCES_NORMATIVA),
                    "tipoNorma", new ToolProp("string", "Tipo de norma", TIPOS_NORMA)
            ));

    private static final ToolDef TOOL_BUSCAR_ESTUDIOS = new ToolDef(
            "buscar_estudios",
            "Busca estudios reales de la plataforma por palabra clave en el título o la descripción, "
                    + "opcionalmente filtrando por estado o año. Úsala también para preguntas cualitativas o "
                    + "temáticas (por ejemplo, si existe algo sobre compostaje, aunque esa palabra no esté en el "
                    + "título). Devuelve una lista que se mostrará como tarjetas: no repitas los títulos en tu "
                    + "texto, solo menciona brevemente que aparecen abajo.",
            props(
                    "palabra_clave", new ToolProp("string", "Palabra o frase a buscar en título o descripción", null),
                    "estado", new ToolProp("string", "Filtrar por estado", ESTADOS_ESTUDIO),
                    "anio", new ToolProp("integer", "Filtrar por año", null)
            ));

    private static final ToolDef TOOL_BUSCAR_NORMATIVAS = new ToolDef(
            "buscar_normativas",
            "Busca normativas reales de la plataforma por palabra clave en título, código, organismo emisor, "
                    + "descripción o campo de aplicación, opcionalmente filtrando por estado o año. Úsala también "
                    + "para preguntas cualitativas o temáticas, no solo cuando la palabra clave coincide con el "
                    + "título. Devuelve una lista que se mostrará como tarjetas: no repitas los títulos en tu "
                    + "texto, solo menciona brevemente que aparecen abajo.",
            props(
                    "palabra_clave", new ToolProp("string", "Palabra o frase a buscar", null),
                    "estado", new ToolProp("string", "Filtrar por estado", ESTADOS_NORMATIVA),
                    "anio", new ToolProp("integer", "Filtrar por año", null)
            ));

    private static final ToolDef TOOL_TENDENCIA_ESTUDIOS = new ToolDef(
            "tendencia_estudios_por_anio",
            "Devuelve cuántos estudios hay por cada año, opcionalmente filtrando por estado. Úsala para preguntas "
                    + "sobre evolución en el tiempo, comparaciones entre años, o en qué año hubo más o menos estudios.",
            props("estado", new ToolProp("string", "Filtrar por estado", ESTADOS_ESTUDIO)));

    private static final ToolDef TOOL_TENDENCIA_NORMATIVAS = new ToolDef(
            "tendencia_normativas_por_anio",
            "Devuelve cuántas normativas hay por cada año, opcionalmente filtrando por estado. Úsala para preguntas "
                    + "sobre evolución en el tiempo, comparaciones entre años, o en qué año hubo más o menos normativas.",
            props("estado", new ToolProp("string", "Filtrar por estado", ESTADOS_NORMATIVA)));

    private static final ToolDef TOOL_ESTUDIOS_RECIENTES = new ToolDef(
            "estudios_recientes",
            "Devuelve los estudios publicados en los últimos N días (por defecto 30 si no se especifica). "
                    + "Úsala para preguntas sobre qué se agregó recientemente, el último mes, la última semana, etc. "
                    + "Los resultados se muestran como tarjetas.",
            props("dias", new ToolProp("integer", "Días hacia atrás a considerar, por defecto 30", null)));

    private static final ToolDef TOOL_NORMATIVAS_RECIENTES = new ToolDef(
            "normativas_recientes",
            "Devuelve las normativas agregadas en los últimos N días (por defecto 30 si no se especifica). "
                    + "Úsala para preguntas sobre qué se agregó recientemente, el último mes, la última semana, etc. "
                    + "Los resultados se muestran como tarjetas.",
            props("dias", new ToolProp("integer", "Días hacia atrás a considerar, por defecto 30", null)));

    private static final ToolDef TOOL_CONTAR_USUARIOS = new ToolDef(
            "contar_usuarios",
            "Cuenta usuarios de la plataforma (socios y/o visualizadores) según rol y/o estado de cuenta.",
            props(
                    "rol", new ToolProp("string", "Rol a filtrar", List.of("SOCIO", "VISUALIZADOR")),
                    "estado", new ToolProp("string", "Estado de la cuenta", List.of("ACTIVO", "BLOQUEADO", "PENDIENTE"))
            ));

    private static final ToolDef TOOL_CONTAR_SOLICITUDES = new ToolDef(
            "contar_solicitudes_registro",
            "Cuenta solicitudes de registro de nuevos usuarios según su estado.",
            props("estado", new ToolProp("string", "Estado de la solicitud", List.of("PENDIENTE", "APROBADO", "RECHAZADO"))));

    private static final ToolDef TOOL_ESTADISTICAS_DESCARGAS = new ToolDef(
            "estadisticas_descargas",
            "Cuenta descargas registradas en la plataforma, opcionalmente filtrando por tipo de documento.",
            props("tipo", new ToolProp("string", "Tipo de documento", List.of("ESTUDIO", "NORMATIVA"))));

    private static final ToolDef TOOL_CONTAR_ADMINISTRADORES = new ToolDef(
            "contar_administradores",
            "Cuenta cuentas de administrador registradas, opcionalmente filtrando por estado.",
            props("estado", new ToolProp("string", "Estado de la cuenta", List.of("ACTIVO", "BLOQUEADO"))));

    private static final ToolDef TOOL_ESTADO_SEGURIDAD = new ToolDef(
            "estado_seguridad",
            "Devuelve el estado de seguridad del sistema: dominios autorizados activos/inactivos, "
                    + "intentos fallidos de login en las últimas 24 horas y la política de contraseñas vigente.",
            Map.of());

    private static final List<String> ROLES_CATALOGO_PUBLICO = List.of("SOCIO", "VISUALIZADOR", "ADMIN");

    /** Devuelve las herramientas visibles para el rol actual: cada rol solo ve las de su propio dominio. */
    private List<ToolDef> herramientasParaRol(String rol) {
        List<ToolDef> tools = new java.util.ArrayList<>();
        if (ROLES_CATALOGO_PUBLICO.contains(rol)) {
            tools.add(TOOL_CONTAR_ESTUDIOS);
            tools.add(TOOL_CONTAR_NORMATIVAS);
            tools.add(TOOL_BUSCAR_ESTUDIOS);
            tools.add(TOOL_BUSCAR_NORMATIVAS);
            tools.add(TOOL_TENDENCIA_ESTUDIOS);
            tools.add(TOOL_TENDENCIA_NORMATIVAS);
            tools.add(TOOL_ESTUDIOS_RECIENTES);
            tools.add(TOOL_NORMATIVAS_RECIENTES);
        }
        if ("ADMIN".equals(rol)) {
            tools.add(TOOL_CONTAR_USUARIOS);
            tools.add(TOOL_CONTAR_SOLICITUDES);
            tools.add(TOOL_ESTADISTICAS_DESCARGAS);
        }
        if ("SUPERADMIN".equals(rol)) {
            tools.add(TOOL_CONTAR_ADMINISTRADORES);
            tools.add(TOOL_ESTADO_SEGURIDAD);
        }
        return tools;
    }

    /**
     * Ejecuta una herramienta pedida por el modelo y devuelve un resultado serializable a JSON.
     * Si la herramienta es una búsqueda, además acumula las tarjetas encontradas en recursosAcumulados.
     */
    private Object ejecutarHerramienta(String nombre, JsonNode args, String rol, List<RecursoBD> recursosAcumulados) {
        try {
            switch (nombre) {
                case "contar_estudios": {
                    String estado  = textoONull(args, "estado");
                    Integer anio   = enteroONull(args, "anio");
                    String formato = textoONull(args, "formato");
                    long total = estudioRepo.findAll().stream()
                            .filter(e -> e.getEliminadoEn() == null)
                            .filter(e -> estado == null || e.getEstado().name().equalsIgnoreCase(estado))
                            .filter(e -> anio == null || anio.equals(e.getAnio()))
                            .filter(e -> formato == null || e.getFormato().name().equalsIgnoreCase(formato))
                            .count();
                    return Map.of("total", total);
                }
                case "contar_normativas": {
                    String estado    = textoONull(args, "estado");
                    Integer anio     = enteroONull(args, "anio");
                    String alcance   = textoONull(args, "alcance");
                    String tipoNorma = textoONull(args, "tipoNorma");
                    long total = normativaRepo.findAllNormativas().stream()
                            .filter(n -> n.getEliminadoEn() == null)
                            .filter(n -> estado == null || n.getEstado().name().equalsIgnoreCase(estado))
                            .filter(n -> anio == null || anio.equals(n.getAnio()))
                            .filter(n -> alcance == null || n.getAlcance().name().equalsIgnoreCase(alcance))
                            .filter(n -> tipoNorma == null || n.getTipoNorma().name().equalsIgnoreCase(tipoNorma))
                            .count();
                    return Map.of("total", total);
                }
                case "buscar_estudios": {
                    String palabraClave = textoONull(args, "palabra_clave");
                    String estado       = textoONull(args, "estado");
                    Integer anio        = enteroONull(args, "anio");
                    String pcLower      = palabraClave == null ? null : palabraClave.toLowerCase();
                    List<Estudio> encontrados = estudioRepo.findAll().stream()
                            .filter(e -> e.getEliminadoEn() == null)
                            .filter(e -> pcLower == null || pcLower.isBlank()
                                    || e.getTitulo().toLowerCase().contains(pcLower)
                                    || e.getDescripcion().toLowerCase().contains(pcLower))
                            .filter(e -> estado == null || e.getEstado().name().equalsIgnoreCase(estado))
                            .filter(e -> anio == null || anio.equals(e.getAnio()))
                            .limit(5)
                            .collect(Collectors.toList());
                    for (Estudio e : encontrados) {
                        recursosAcumulados.add(new RecursoBD(e.getIdEstudio(), "ESTUDIO",
                                e.getTitulo(), e.getDescripcion(), null, e.getAnio()));
                    }
                    return Map.of("total_encontrados", encontrados.size(), "resultados", encontrados.stream()
                            .map(e -> Map.of("id", e.getIdEstudio(), "titulo", e.getTitulo(),
                                    "anio", e.getAnio(), "estado", e.getEstado().name()))
                            .collect(Collectors.toList()));
                }
                case "buscar_normativas": {
                    String palabraClave = textoONull(args, "palabra_clave");
                    String estado       = textoONull(args, "estado");
                    Integer anio        = enteroONull(args, "anio");
                    String pcLower      = palabraClave == null ? null : palabraClave.toLowerCase();
                    List<Normativa> encontradas = normativaRepo.findAllNormativas().stream()
                            .filter(n -> n.getEliminadoEn() == null)
                            .filter(n -> pcLower == null || pcLower.isBlank()
                                    || n.getTitulo().toLowerCase().contains(pcLower)
                                    || (n.getCodigo() != null && n.getCodigo().toLowerCase().contains(pcLower))
                                    || n.getOrganismoEmisor().toLowerCase().contains(pcLower)
                                    || (n.getDescripcion() != null && n.getDescripcion().toLowerCase().contains(pcLower))
                                    || (n.getCampoAplicacion() != null && n.getCampoAplicacion().toLowerCase().contains(pcLower)))
                            .filter(n -> estado == null || n.getEstado().name().equalsIgnoreCase(estado))
                            .filter(n -> anio == null || anio.equals(n.getAnio()))
                            .limit(5)
                            .collect(Collectors.toList());
                    for (Normativa n : encontradas) {
                        recursosAcumulados.add(new RecursoBD(n.getIdNormativa(), "NORMATIVA",
                                n.getTitulo(), n.getDescripcion(), n.getCodigo(), n.getAnio()));
                    }
                    return Map.of("total_encontradas", encontradas.size(), "resultados", encontradas.stream()
                            .map(n -> Map.of("id", n.getIdNormativa(), "titulo", n.getTitulo(),
                                    "anio", n.getAnio(), "estado", n.getEstado().name()))
                            .collect(Collectors.toList()));
                }
                case "tendencia_estudios_por_anio": {
                    String estado = textoONull(args, "estado");
                    List<Map<String, Object>> porAnio = estudioRepo.findAll().stream()
                            .filter(e -> e.getEliminadoEn() == null)
                            .filter(e -> estado == null || e.getEstado().name().equalsIgnoreCase(estado))
                            .collect(Collectors.groupingBy(Estudio::getAnio, Collectors.counting()))
                            .entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(e -> Map.<String, Object>of("anio", e.getKey(), "total", e.getValue()))
                            .collect(Collectors.toList());
                    return Map.of("por_anio", porAnio);
                }
                case "tendencia_normativas_por_anio": {
                    String estado = textoONull(args, "estado");
                    List<Map<String, Object>> porAnio = normativaRepo.findAllNormativas().stream()
                            .filter(n -> n.getEliminadoEn() == null)
                            .filter(n -> estado == null || n.getEstado().name().equalsIgnoreCase(estado))
                            .collect(Collectors.groupingBy(Normativa::getAnio, Collectors.counting()))
                            .entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(e -> Map.<String, Object>of("anio", e.getKey(), "total", e.getValue()))
                            .collect(Collectors.toList());
                    return Map.of("por_anio", porAnio);
                }
                case "estudios_recientes": {
                    Integer dias = enteroONull(args, "dias");
                    int diasFinal = dias != null ? dias : 30;
                    LocalDate limite = LocalDate.now().minusDays(diasFinal);
                    List<Estudio> recientes = estudioRepo.findAll().stream()
                            .filter(e -> e.getEliminadoEn() == null)
                            .filter(e -> e.getFechaPublicacion() != null && !e.getFechaPublicacion().isBefore(limite))
                            .sorted(Comparator.comparing(Estudio::getFechaPublicacion).reversed())
                            .limit(5)
                            .collect(Collectors.toList());
                    for (Estudio e : recientes) {
                        recursosAcumulados.add(new RecursoBD(e.getIdEstudio(), "ESTUDIO",
                                e.getTitulo(), e.getDescripcion(), null, e.getAnio()));
                    }
                    return Map.of("dias", diasFinal, "total_encontrados", recientes.size(), "resultados", recientes.stream()
                            .map(e -> Map.of("id", e.getIdEstudio(), "titulo", e.getTitulo(),
                                    "fecha_publicacion", String.valueOf(e.getFechaPublicacion())))
                            .collect(Collectors.toList()));
                }
                case "normativas_recientes": {
                    Integer dias = enteroONull(args, "dias");
                    int diasFinal = dias != null ? dias : 30;
                    LocalDateTime limite = LocalDateTime.now().minusDays(diasFinal);
                    List<Normativa> recientes = normativaRepo.findAllNormativas().stream()
                            .filter(n -> n.getEliminadoEn() == null)
                            .filter(n -> n.getFechaCreacion() != null && !n.getFechaCreacion().isBefore(limite))
                            .sorted(Comparator.comparing(Normativa::getFechaCreacion).reversed())
                            .limit(5)
                            .collect(Collectors.toList());
                    for (Normativa n : recientes) {
                        recursosAcumulados.add(new RecursoBD(n.getIdNormativa(), "NORMATIVA",
                                n.getTitulo(), n.getDescripcion(), n.getCodigo(), n.getAnio()));
                    }
                    return Map.of("dias", diasFinal, "total_encontradas", recientes.size(), "resultados", recientes.stream()
                            .map(n -> Map.of("id", n.getIdNormativa(), "titulo", n.getTitulo(),
                                    "fecha_creacion", String.valueOf(n.getFechaCreacion())))
                            .collect(Collectors.toList()));
                }
                case "contar_usuarios": {
                    // Solo tiene sentido para ADMIN; se valida arriba con herramientasParaRol
                    String rolFiltro   = textoONull(args, "rol");
                    String estadoFiltro = textoONull(args, "estado");
                    List<Integer> rolIds = "SOCIO".equalsIgnoreCase(rolFiltro) ? List.of(3)
                            : "VISUALIZADOR".equalsIgnoreCase(rolFiltro) ? List.of(4)
                            : List.of(3, 4);
                    if ("ACTIVO".equalsIgnoreCase(estadoFiltro)) {
                        return Map.of("total", usuarioRepo.countActiveAdminsByRole(rolIds));
                    } else if ("BLOQUEADO".equalsIgnoreCase(estadoFiltro)) {
                        return Map.of("total", usuarioRepo.countBlockedAdminsByRole(rolIds));
                    } else if ("PENDIENTE".equalsIgnoreCase(estadoFiltro)) {
                        return Map.of("total", usuarioRepo.countByEstadoAprobacionAndEliminadoEnIsNull("PENDIENTE"));
                    }
                    return Map.of("total", usuarioRepo.countByRol_IdInAndEliminadoEnIsNull(rolIds));
                }
                case "contar_solicitudes_registro": {
                    String estado = textoONull(args, "estado");
                    if (estado == null) {
                        long pend = solicitudRepo.countByEstado("PENDIENTE");
                        long apro = solicitudRepo.countByEstado("APROBADO");
                        long rech = solicitudRepo.countByEstado("RECHAZADO");
                        return Map.of("pendientes", pend, "aprobadas", apro, "rechazadas", rech, "total", pend + apro + rech);
                    }
                    return Map.of("total", solicitudRepo.countByEstado(estado.toUpperCase()));
                }
                case "estadisticas_descargas": {
                    String tipo = textoONull(args, "tipo");
                    if (tipo == null) {
                        return Map.of("total", registroDescargaRepo.count(),
                                "estudios", registroDescargaRepo.countByTipoDocumento("ESTUDIO"),
                                "normativas", registroDescargaRepo.countByTipoDocumento("NORMATIVA"));
                    }
                    return Map.of("total", registroDescargaRepo.countByTipoDocumento(tipo.toUpperCase()));
                }
                case "contar_administradores": {
                    String estado = textoONull(args, "estado");
                    List<Integer> adminIds = List.of(2);
                    if ("ACTIVO".equalsIgnoreCase(estado)) return Map.of("total", usuarioRepo.countActiveAdminsByRole(adminIds));
                    if ("BLOQUEADO".equalsIgnoreCase(estado)) return Map.of("total", usuarioRepo.countBlockedAdminsByRole(adminIds));
                    return Map.of("total", usuarioRepo.countByRol_IdInAndEliminadoEnIsNull(adminIds));
                }
                case "estado_seguridad": {
                    long dominiosActivos   = dominioRepo.countByEstadoTrue();
                    long dominiosInactivos = dominioRepo.countByEstadoFalse();
                    long intentosFallidos  = intentoLoginRepo.countByFechaAfterAndExitosoFalse(LocalDateTime.now().minusHours(24));
                    var politica = politicaRepo.findAll().stream().findFirst();
                    String infoPolitica = politica.map(p ->
                            "longitud mínima %d, requiere mayúsculas: %s, requiere números: %s, requiere especiales: %s"
                                    .formatted(p.getLongitudMinima(),
                                            p.getRequiereMayuscula() ? "sí" : "no",
                                            p.getRequiereNumero() ? "sí" : "no",
                                            p.getRequiereSimbolo() ? "sí" : "no")
                    ).orElse("no configurada");
                    return Map.of(
                            "dominios_activos", dominiosActivos,
                            "dominios_inactivos", dominiosInactivos,
                            "intentos_fallidos_24h", intentosFallidos,
                            "politica_contrasenas", infoPolitica
                    );
                }
                default:
                    return Map.of("error", "Herramienta desconocida: " + nombre);
            }
        } catch (Exception e) {
            log.warn("[CHATBOT] Error ejecutando herramienta {}: {}", nombre, e.getMessage());
            return Map.of("error", "No se pudo completar la consulta: " + e.getMessage());
        }
    }

    private String textoONull(JsonNode args, String campo) {
        if (args == null || !args.hasNonNull(campo)) return null;
        String v = args.get(campo).asText();
        return v.isBlank() ? null : v;
    }

    private Integer enteroONull(JsonNode args, String campo) {
        if (args == null || !args.hasNonNull(campo)) return null;
        return args.get(campo).asInt();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONTEXTOS POR ROL
    // ─────────────────────────────────────────────────────────────────────────

    /** Contexto SOCIO: carpetas de Mi Espacio + contenido guardado para recomendar dónde guardar. */
    private String construirContextoSocio(Long idUsuario) {
        try {
            var carpetas = carpetaRepo.findByUsuario_IdUsuarioOrderByNombreAsc(idUsuario);
            if (carpetas.isEmpty()) {
                return "\n\n--- MI ESPACIO DEL USUARIO ---\n"
                        + "El usuario todavía NO tiene ninguna carpeta creada en su Mi Espacio.\n"
                        + "Si pregunta dónde guardar un documento, dile que aún no tiene carpetas y sugiérele "
                        + "un nombre de carpeta acorde al tema del documento para que la cree primero.\n"
                        + "--- FIN MI ESPACIO ---\n";
            }

            // Contar cuántos items tiene cada carpeta
            var conteoMap = new java.util.HashMap<Long, Long>();
            contenidoRepo.findByUsuario_IdUsuarioOrderByFechaGuardadoDesc(idUsuario)
                    .forEach(c -> {
                        if (c.getCarpeta() != null) {
                            conteoMap.merge(c.getCarpeta().getIdCarpeta(), 1L, Long::sum);
                        }
                    });

            long totalGuardados = contenidoRepo.countByUsuario_IdUsuario(idUsuario);
            long estudiosGuardados = contenidoRepo.countByUsuario_IdUsuarioAndTipoDocumento(idUsuario, "ESTUDIO");
            long normativasGuardadas = contenidoRepo.countByUsuario_IdUsuarioAndTipoDocumento(idUsuario, "NORMATIVA");

            StringBuilder sb = new StringBuilder();
            sb.append("\n\n--- MI ESPACIO DEL USUARIO ---\n");
            sb.append("El usuario tiene ").append(carpetas.size()).append(" carpetas en su Mi Espacio. ");
            sb.append("Total guardados: ").append(totalGuardados)
              .append(" (estudios: ").append(estudiosGuardados)
              .append(", normativas: ").append(normativasGuardadas).append(")\n");
            sb.append("Sus carpetas son:\n");
            for (var c : carpetas) {
                long items = conteoMap.getOrDefault(c.getIdCarpeta(), 0L);
                sb.append("- \"").append(c.getNombre()).append("\"");
                if (c.getEmoji() != null) sb.append(" ").append(c.getEmoji());
                sb.append(" (").append(items).append(" items)\n");
            }
            sb.append("\nCuando el usuario pregunte dónde guardar un documento o pida recomendaciones, ");
            sb.append("sugiere la carpeta más adecuada por su nombre. Si ninguna encaja, sugiere crear una nueva.\n");
            sb.append("--- FIN MI ESPACIO ---\n");
            return sb.toString();

        } catch (Exception e) {
            log.warn("[CHATBOT] Error construyendo contexto socio: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Construye contexto con estadísticas reales de la plataforma para el rol ADMIN,
     * usando los mismos datos que sus propias vistas: Gestión de Usuarios, Solicitudes,
     * Estudios, Normativas y Reportes de Uso (ingresos y descargas).
     */
    private String construirContextoAdmin() {
        try {
            // Usuarios gestionados por ADMIN: SOCIO(3) y VISUALIZADOR(4) — nunca ADMIN/SUPERADMIN
            List<Integer> rolUsuariosPlataforma = List.of(3, 4);
            long totalSocios         = usuarioRepo.countByRol_IdInAndEliminadoEnIsNull(List.of(3));
            long totalVisualizadores = usuarioRepo.countByRol_IdInAndEliminadoEnIsNull(List.of(4));
            long usuariosActivos     = usuarioRepo.countActiveAdminsByRole(rolUsuariosPlataforma);
            long usuariosBloqueados  = usuarioRepo.countBlockedAdminsByRole(rolUsuariosPlataforma);
            long pendientesAprobacion = usuarioRepo.countByEstadoAprobacionAndEliminadoEnIsNull("PENDIENTE");
            long solicitudesPendientes = solicitudRepo.countByEstado("PENDIENTE");

            long totalEstudios    = estudioRepo.countByEliminadoEnIsNull();
            long totalNormativas  = normativaRepo.findAllNormativas().stream()
                    .filter(n -> n.getEliminadoEn() == null).count();
            long estudiosVigentesAdmin = estudioRepo.countByEstadoAndEliminadoEnIsNull(
                    Estudio.EstadoEstudio.VIGENTE);

            // Actividad — mismos datos que "Reportes de Uso"
            LocalDateTime inicioDia    = LocalDate.now().atStartOfDay();
            LocalDateTime inicioSemana = LocalDate.now().minusDays(6).atStartOfDay();
            long ingresosHoy    = registroSesionRepo.countByFechaInicioAfter(inicioDia);
            long ingresosSemana = registroSesionRepo.countByFechaInicioAfter(inicioSemana);

            // Descargas — mismos datos que "Registros de Descargas"
            long totalDescargas     = registroDescargaRepo.count();
            long descargasEstudio   = registroDescargaRepo.countByTipoDocumento("ESTUDIO");
            long descargasNormativa = registroDescargaRepo.countByTipoDocumento("NORMATIVA");

            StringBuilder alertas = new StringBuilder();
            if (pendientesAprobacion > 0)
                alertas.append("ALERTA: ").append(pendientesAprobacion).append(" usuarios pendientes de aprobación.\n");
            if (solicitudesPendientes > 0)
                alertas.append("ALERTA: ").append(solicitudesPendientes).append(" solicitudes de registro sin revisar.\n");
            if (usuariosBloqueados > 5)
                alertas.append("ALERTA: ").append(usuariosBloqueados).append(" usuarios bloqueados, considera revisarlos.\n");

            return """

                    --- ESTADÍSTICAS ACTUALES (ADMIN) ---
                    %s
                    Usuarios (socios y visualizadores):
                    - Total: %d (socios: %d, visualizadores: %d)
                    - Activos: %d, bloqueados: %d
                    - Pendientes de aprobación: %d
                    - Solicitudes de registro pendientes: %d

                    Contenido:
                    - Estudios: %d (vigentes: %d)
                    - Normativas: %d

                    Actividad e ingresos:
                    - Ingresos hoy: %d
                    - Ingresos esta semana: %d

                    Descargas:
                    - Total: %d (estudios: %d, normativas: %d)

                    CAPACIDADES ESPECIALES PARA ADMIN:
                    - Si el admin pide redactar una notificación, genera TITULO (máx 80 chars) y MENSAJE (máx 300 chars) listos para copiar y enviar.
                    - Sugiere proactivamente acciones si hay alertas pendientes.
                    --- FIN ESTADÍSTICAS ---
                    """.formatted(alertas.toString(),
                    totalSocios + totalVisualizadores, totalSocios, totalVisualizadores,
                    usuariosActivos, usuariosBloqueados,
                    pendientesAprobacion, solicitudesPendientes,
                    totalEstudios, estudiosVigentesAdmin, totalNormativas,
                    ingresosHoy, ingresosSemana,
                    totalDescargas, descargasEstudio, descargasNormativa);

        } catch (Exception e) {
            log.warn("[CHATBOT] Error construyendo contexto admin: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Construye el contexto del rol SUPERADMIN, limitado ESTRICTAMENTE a su propio dominio:
     * administradores, seguridad y estado del sistema (lo mismo que ve en su sidebar:
     * Dashboard, Administradores, Configuración de Seguridad, Estado del Sistema).
     * NO incluye socios/visualizadores, solicitudes, estudios ni normativas: eso es de ADMIN.
     */
    private String construirContextoSuperadmin() {
        try {
            // Administradores — mismo filtro de rol que usa la vista "Administradores" (ROL_ADMIN_IDS = ADMIN)
            java.util.List<Integer> rolAdminIds = java.util.List.of(2); // ADMIN (no incluye SOCIO ni al propio SUPERADMIN)
            long totalAdmins      = usuarioRepo.countByRol_IdInAndEliminadoEnIsNull(rolAdminIds);
            long adminsActivos    = usuarioRepo.countActiveAdminsByRole(rolAdminIds);
            long adminsBloqueados = usuarioRepo.countBlockedAdminsByRole(rolAdminIds);

            // Seguridad — mismos datos que "Configuración de Seguridad"
            long dominiosActivos   = dominioRepo.countByEstadoTrue();
            long dominiosInactivos = dominioRepo.countByEstadoFalse();
            long intentosFallidos  = intentoLoginRepo.countByFechaAfterAndExitosoFalse(
                    java.time.LocalDateTime.now().minusHours(24));
            var politica = politicaRepo.findAll().stream().findFirst();
            String infoPolitica = politica.map(p ->
                    "longitud mínima %d, requiere mayúsculas: %s, requiere números: %s, requiere especiales: %s"
                    .formatted(p.getLongitudMinima(),
                            p.getRequiereMayuscula() ? "sí" : "no",
                            p.getRequiereNumero() ? "sí" : "no",
                            p.getRequiereSimbolo() ? "sí" : "no")
            ).orElse("no configurada");

            // Estado del sistema — mismo dato que "Estado del Sistema"
            long sesionesActivas = registroSesionRepo.countUsuariosActivos();

            // Alertas propias del dominio superadmin
            StringBuilder alertasSA = new StringBuilder();
            if (intentosFallidos > 20)
                alertasSA.append("ALERTA SEGURIDAD: ").append(intentosFallidos).append(" intentos fallidos de login en 24h.\n");
            if (adminsBloqueados > 0)
                alertasSA.append("ATENCIÓN: ").append(adminsBloqueados).append(" administradores bloqueados.\n");

            return """

                    --- ESTADO DEL SISTEMA (SUPERADMIN) ---
                    %s
                    ADMINISTRADORES:
                    - Total: %d (activos: %d, bloqueados: %d)

                    SEGURIDAD:
                    - Dominios autorizados: %d activos, %d inactivos
                    - Intentos fallidos de login (24h): %d
                    - Política de contraseñas: %s

                    SESIONES:
                    - Sesiones activas ahora mismo: %d

                    CHATBOT IA:
                    - Proveedor: %s | Modelo: %s

                    ALCANCE DE ESTE ROL:
                    Como SUPERADMIN NO tienes datos de socios, visualizadores, estudios, normativas ni \
                    solicitudes de registro: eso pertenece exclusivamente al panel de ADMIN. Si preguntan \
                    por esos temas, indica que deben revisarlo desde el rol Administrador.

                    INSTRUCCIONES DE ANÁLISIS:
                    - Si el superadmin pide análisis o tendencias, interpreta estos números y da recomendaciones concretas.
                    - Si hay alertas, menciónalas proactivamente aunque no se pregunten.
                    --- FIN ESTADO DEL SISTEMA ---
                    """.formatted(
                    alertasSA.toString(),
                    totalAdmins, adminsActivos, adminsBloqueados,
                    dominiosActivos, dominiosInactivos,
                    intentosFallidos, infoPolitica,
                    sesionesActivas,
                    aiProvider, aiProvider.equalsIgnoreCase("GEMINI") ? geminiModel : claudeModel);

        } catch (Exception e) {
            log.warn("[CHATBOT] Error construyendo contexto superadmin: {}", e.getMessage());
            return ""; // sin fallback a construirContextoAdmin(): eso violaría el aislamiento de roles
        }
    }

    private String llamarClaudeApi(List<ChatMensaje> contexto, String contextoBD, String rol,
                                    Long idUsuario, List<RecursoBD> recursosAcumulados) throws Exception {
        List<ToolDef> tools = herramientasParaRol(rol);

        ObjectNode body = mapper.createObjectNode();
        body.put("model", claudeModel);
        body.put("max_tokens", 1024);
        body.put("system", SYSTEM_PROMPT + contextoBD);

        ArrayNode messages = body.putArray("messages");
        for (ChatMensaje m : contexto) {
            ObjectNode msg = messages.addObject();
            msg.put("role", m.getEmisor() == EmisorMensaje.USUARIO ? "user" : "assistant");
            msg.put("content", m.getMensaje());
        }

        if (!tools.isEmpty()) {
            ArrayNode toolsArr = body.putArray("tools");
            for (ToolDef t : tools) toolsArr.add(t.aClaudeJson(mapper));
        }

        for (int ronda = 0; ronda < MAX_RONDAS_HERRAMIENTAS; ronda++) {
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
                throw new RuntimeException("Claude API error " + response.statusCode() + ": " + response.body());
            }

            JsonNode json = mapper.readTree(response.body());
            JsonNode contentArr = json.path("content");

            StringBuilder texto = new StringBuilder();
            List<JsonNode> toolUses = new java.util.ArrayList<>();
            for (JsonNode block : contentArr) {
                String tipo = block.path("type").asText();
                if ("text".equals(tipo)) texto.append(block.path("text").asText());
                if ("tool_use".equals(tipo)) toolUses.add(block);
            }

            if (toolUses.isEmpty()) {
                return texto.toString();
            }

            log.info("[CHATBOT] Claude pidió {} herramienta(s) en ronda {}", toolUses.size(), ronda + 1);

            ObjectNode assistantMsg = messages.addObject();
            assistantMsg.put("role", "assistant");
            assistantMsg.set("content", contentArr);

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            ArrayNode resultParts = userMsg.putArray("content");
            for (JsonNode tu : toolUses) {
                String nombreFuncion = tu.path("name").asText();
                String toolUseId     = tu.path("id").asText();
                JsonNode args        = tu.path("input");
                Object resultado = ejecutarHerramienta(nombreFuncion, args, rol, recursosAcumulados);

                ObjectNode resultBlock = resultParts.addObject();
                resultBlock.put("type", "tool_result");
                resultBlock.put("tool_use_id", toolUseId);
                resultBlock.put("content", mapper.writeValueAsString(resultado));
            }
        }

        throw new RuntimeException("Se excedió el número máximo de llamadas a herramientas");
    }

    private String llamarGemini(List<ChatMensaje> contexto, String contextoBD, String rol,
                                 Long idUsuario, List<RecursoBD> recursosAcumulados) throws Exception {
        List<ToolDef> tools = herramientasParaRol(rol);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        ObjectNode body = mapper.createObjectNode();

        // System instruction para Gemini (incluye contexto de BD si lo hay)
        ObjectNode systemInstruction = body.putObject("system_instruction");
        ArrayNode siParts = systemInstruction.putArray("parts");
        siParts.addObject().put("text", SYSTEM_PROMPT + contextoBD);

        // Historial de conversación
        ArrayNode contents = body.putArray("contents");
        for (ChatMensaje m : contexto) {
            ObjectNode content = contents.addObject();
            content.put("role", m.getEmisor() == EmisorMensaje.USUARIO ? "user" : "model");
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", m.getMensaje());
        }

        if (!tools.isEmpty()) {
            ArrayNode toolsArr = body.putArray("tools");
            ObjectNode toolObj = toolsArr.addObject();
            ArrayNode funcDecls = toolObj.putArray("functionDeclarations");
            for (ToolDef t : tools) funcDecls.add(t.aGeminiJson(mapper));
        }

        ObjectNode genConfig = body.putObject("generationConfig");
        genConfig.put("maxOutputTokens", 1024);
        genConfig.put("temperature", 0.7);

        for (int ronda = 0; ronda < MAX_RONDAS_HERRAMIENTAS; ronda++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini API error " + response.statusCode() + ": " + response.body());
            }

            JsonNode json = mapper.readTree(response.body());
            JsonNode partsNode = json.path("candidates").get(0).path("content").path("parts");

            JsonNode funcCallPart = null;
            StringBuilder texto = new StringBuilder();
            for (JsonNode p : partsNode) {
                if (p.has("functionCall")) { funcCallPart = p.get("functionCall"); break; }
                if (p.has("text")) texto.append(p.get("text").asText());
            }

            if (funcCallPart == null) {
                return texto.toString();
            }

            String nombreFuncion = funcCallPart.path("name").asText();
            JsonNode args = funcCallPart.path("args");
            log.info("[CHATBOT] Gemini pidió herramienta '{}' en ronda {}", nombreFuncion, ronda + 1);
            Object resultado = ejecutarHerramienta(nombreFuncion, args, rol, recursosAcumulados);

            // Turno "model" con la llamada a función tal cual la devolvió el modelo
            ObjectNode modelTurn = contents.addObject();
            modelTurn.put("role", "model");
            modelTurn.putArray("parts").addObject().set("functionCall", funcCallPart);

            // Turno "function" con el resultado real de la consulta
            ObjectNode funcTurn = contents.addObject();
            funcTurn.put("role", "function");
            ObjectNode funcResponse = funcTurn.putArray("parts").addObject().putObject("functionResponse");
            funcResponse.put("name", nombreFuncion);
            funcResponse.set("response", mapper.createObjectNode().set("result", mapper.valueToTree(resultado)));
        }

        throw new RuntimeException("Se excedió el número máximo de llamadas a herramientas");
    }
}


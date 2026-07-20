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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    // Límite de mensajes por sesión para evitar abuso
    private static final int LIMITE_MENSAJES_SESION = 50;
    // Cuántos mensajes anteriores se envían como contexto a la IA
    private static final int CONTEXTO_MENSAJES      = 10;

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
            - Si el contexto menciona recursos de la plataforma, NO los listes tú mismo. Solo menciona que aparecerán como tarjetas.
            - Para notificaciones: cuando redactes, presenta TITULO y MENSAJE claramente separados.
            - No inventes cifras; usa solo los datos del contexto provisto.
            - Máximo 250 palabras salvo que pidan más detalle.
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

        // 4c. Buscar recursos en BD (estudios/normativas): SUPERADMIN queda excluido,
        // ese catálogo pertenece a las vistas de ADMIN/SOCIO/VISUALIZADOR, no a las suyas.
        BusquedaBDResultado busqueda = "SUPERADMIN".equals(rolActual)
                ? new BusquedaBDResultado("", List.of())
                : buscarRecursosBD(textoUsuario);

        // 4d. Agregar contexto propio del rol (cada uno ve SOLO su dominio)
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

        String contextoFinal = busqueda.contextoPrompt + directivaRol + contextoRol;

        // 5. Llamar a la IA (con retry automático si falla por carga)
        String respuesta = null;
        int intentos = 0;
        String contextoActual = contextoFinal;
        while (intentos < 3 && respuesta == null) {
            try {
                respuesta = switch (aiProvider.toUpperCase()) {
                    case "CLAUDE_API" -> llamarClaudeApi(contexto, contextoActual);
                    case "GEMINI"     -> llamarGemini(contexto, contextoActual);
                    default           -> llamarGemini(contexto, contextoActual);
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

        return new ChatbotResultado(respuesta, busqueda.recursos);
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
    // PROVEEDORES DE IA
    // ─────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    // BÚSQUEDA DE CONTEXTO EN BD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca estudios y normativas relevantes al mensaje del usuario
     * y construye un bloque de contexto para enriquecer el prompt.
     */
    // Palabras que indican que el usuario pregunta por el catálogo completo
    private static final List<String> PALABRAS_CATALOGO_NORM = List.of(
            "normativa", "normativas", "norma", "normas", "ley", "leyes",
            "reglamento", "reglamentos", "decreto", "decretos", "resolución", "resoluciones"
    );
    private static final List<String> PALABRAS_CATALOGO_EST = List.of(
            "estudio", "estudios", "documento", "documentos", "informe", "informes",
            "investigación", "investigaciones", "reporte", "reportes"
    );
    private static final List<String> PALABRAS_LISTADO = List.of(
            "qué", "que", "cuales", "cuáles", "cuantos", "cuántos",
            "hay", "tienen", "tienes", "existe", "existen", "disponible",
            "disponibles", "listar", "mostrar", "ver", "lista"
    );

    /** Resultado interno de la búsqueda en BD. */
    private static class BusquedaBDResultado {
        final String           contextoPrompt;
        final List<RecursoBD>  recursos;
        BusquedaBDResultado(String contextoPrompt, List<RecursoBD> recursos) {
            this.contextoPrompt = contextoPrompt;
            this.recursos       = recursos;
        }
    }

    private BusquedaBDResultado buscarRecursosBD(String mensaje) {
        try {
            String msgLower = mensaje.toLowerCase();

            boolean preguntaNormativas = PALABRAS_CATALOGO_NORM.stream().anyMatch(msgLower::contains)
                    && PALABRAS_LISTADO.stream().anyMatch(msgLower::contains);
            boolean preguntaEstudios   = PALABRAS_CATALOGO_EST.stream().anyMatch(msgLower::contains)
                    && PALABRAS_LISTADO.stream().anyMatch(msgLower::contains);

            List<Estudio>   estudios;
            List<Normativa> normativas;

            if (preguntaNormativas || preguntaEstudios) {
                estudios = preguntaEstudios
                        ? estudioRepo.findAllEstudioDTO().stream().limit(10)
                                .map(dto -> estudioRepo.findById(dto.id()).orElse(null))
                                .filter(e -> e != null && e.getEliminadoEn() == null)
                                .collect(Collectors.toList())
                        : List.of();
                normativas = preguntaNormativas
                        ? normativaRepo.findAllNormativas().stream()
                                .filter(n -> n.getEliminadoEn() == null).limit(10)
                                .collect(Collectors.toList())
                        : List.of();
                log.info("[CHATBOT] Catálogo — norm:{} est:{}", normativas.size(), estudios.size());
            } else {
                String keyword = extraerKeyword(mensaje);
                if (keyword.isBlank()) return new BusquedaBDResultado("", List.of());

                estudios = estudioRepo.findByTituloContainingIgnoreCaseDTO(keyword).stream().limit(3)
                        .map(dto -> estudioRepo.findById(dto.id()).orElse(null))
                        .filter(e -> e != null && e.getEliminadoEn() == null)
                        .collect(Collectors.toList());
                normativas = normativaRepo.findByKeyword(keyword).stream()
                        .filter(n -> n.getEliminadoEn() == null).limit(3)
                        .collect(Collectors.toList());
            }

            if (estudios.isEmpty() && normativas.isEmpty())
                return new BusquedaBDResultado("", List.of());

            // Construir lista de tarjetas
            List<RecursoBD> tarjetas = new java.util.ArrayList<>();
            for (Estudio e : estudios) {
                tarjetas.add(new RecursoBD(e.getIdEstudio(), "ESTUDIO",
                        e.getTitulo(), e.getDescripcion(), null, e.getAnio()));
            }
            for (Normativa n : normativas) {
                tarjetas.add(new RecursoBD(n.getIdNormativa(), "NORMATIVA",
                        n.getTitulo(), n.getDescripcion(), n.getCodigo(), n.getAnio()));
            }

            // Construir texto de contexto para el prompt (solo títulos, sin descripción larga)
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n--- RECURSOS DISPONIBLES EN LA PLATAFORMA ---\n");
            sb.append("Estos recursos reales están en la plataforma. NO los listes en tu respuesta; ");
            sb.append("solo menciona brevemente que existen y que el usuario puede verlos en las tarjetas que aparecerán.\n");
            for (RecursoBD r : tarjetas) {
                sb.append("- [").append(r.tipo).append("] \"").append(r.titulo).append("\" (").append(r.anio).append(")\n");
            }
            sb.append("--- FIN ---\n");

            return new BusquedaBDResultado(sb.toString(), tarjetas);

        } catch (Exception e) {
            log.warn("[CHATBOT] Error buscando recursos BD: {}", e.getMessage());
            return new BusquedaBDResultado("", List.of());
        }
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

    /** Extrae una keyword útil del mensaje del usuario. */
    private String extraerKeyword(String mensaje) {
        // Palabras vacías a ignorar
        List<String> stopWords = List.of("que", "como", "cual", "cuales", "donde", "cuando",
                "hay", "tiene", "tienen", "puedo", "debo", "sobre", "para", "con", "sin",
                "una", "uno", "los", "las", "del", "qué", "cómo", "cuál", "es", "son",
                "me", "te", "se", "la", "le", "un", "en", "de", "el", "y", "a", "o");

        String[] palabras = mensaje.toLowerCase()
                .replaceAll("[¿?¡!.,;:]", "")
                .split("\\s+");

        return java.util.Arrays.stream(palabras)
                .filter(p -> p.length() > 3 && !stopWords.contains(p))
                .findFirst()
                .orElse("");
    }

    private String llamarClaudeApi(List<ChatMensaje> contexto, String contextoBD) throws Exception {
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
        return json.path("content").get(0).path("text").asText();
    }

    private String llamarGemini(List<ChatMensaje> contexto, String contextoBD) throws Exception {
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

        ObjectNode genConfig = body.putObject("generationConfig");
        genConfig.put("maxOutputTokens", 1024);
        genConfig.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error " + response.statusCode() + ": " + response.body());
        }

        JsonNode json = mapper.readTree(response.body());
        return json.path("candidates").get(0)
                   .path("content").path("parts").get(0)
                   .path("text").asText();
    }
}


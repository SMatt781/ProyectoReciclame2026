package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Dto.SessionUserDto;
import com.example.proyectoreciclame.Entity.ChatMensaje;
import com.example.proyectoreciclame.Service.AuthenticatedUserService;
import com.example.proyectoreciclame.Service.ChatbotService;
import com.example.proyectoreciclame.Service.ChatbotService.ChatbotResultado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    @Autowired private ChatbotService          chatbotService;
    @Autowired private AuthenticatedUserService authUserService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /chatbot/mensaje
    // Body: { "mensaje": "texto del usuario" }
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/mensaje")
    public ResponseEntity<?> enviarMensaje(@RequestBody Map<String, String> body) {
        SessionUserDto sessionUser = authUserService.obtenerUsuarioSesion();
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Sesión expirada"));
        }

        String texto = body.get("mensaje");
        if (texto == null || texto.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El mensaje no puede estar vacío"));
        }

        Long idUsuario = sessionUser.getIdUsuario();
        log.info("[CHATBOT] Mensaje de usuario {}: {}", idUsuario, texto.substring(0, Math.min(texto.length(), 80)));

        ChatbotResultado resultado = chatbotService.procesarMensaje(idUsuario, texto.trim());

        if (resultado == null) {
            return ResponseEntity.status(429).body(Map.of(
                    "error",     "Límite de mensajes alcanzado",
                    "mensaje",   "Has alcanzado el límite de mensajes por sesión. Inicia una nueva sesión.",
                    "restantes", 0
            ));
        }

        if ("ERROR_IA".equals(resultado.respuesta)) {
            return ResponseEntity.status(503).body(Map.of(
                    "error",     "El asistente no está disponible en este momento. Intenta en unos segundos.",
                    "restantes", chatbotService.mensajesRestantes(idUsuario)
            ));
        }

        // Serializar recursos como lista de mapas
        var recursos = resultado.recursos.stream().map(r -> {
            var m = new java.util.HashMap<String, Object>();
            m.put("id",          r.id);
            m.put("tipo",        r.tipo);
            m.put("titulo",      r.titulo);
            m.put("descripcion", r.descripcion != null ? r.descripcion : "");
            m.put("codigo",      r.codigo != null ? r.codigo : "");
            m.put("anio",        r.anio);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "respuesta",  resultado.respuesta,
                "recursos",   recursos,
                "restantes",  chatbotService.mensajesRestantes(idUsuario)
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /chatbot/historial  →  devuelve mensajes de la sesión activa
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/historial")
    public ResponseEntity<?> obtenerHistorial() {
        SessionUserDto sessionUser = authUserService.obtenerUsuarioSesion();
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Sesión expirada"));
        }

        List<ChatMensaje> mensajes = chatbotService.obtenerHistorial(sessionUser.getIdUsuario());

        List<Map<String, String>> resultado = mensajes.stream()
                .map(m -> Map.of(
                        "emisor",  m.getEmisor().name(),
                        "mensaje", m.getMensaje(),
                        "fecha",   m.getFecha().toString()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "mensajes",  resultado,
                "restantes", chatbotService.mensajesRestantes(sessionUser.getIdUsuario())
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /chatbot/nueva-sesion  →  cierra la sesión activa y crea una nueva
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/nueva-sesion")
    public ResponseEntity<?> nuevaSesion() {
        SessionUserDto sessionUser = authUserService.obtenerUsuarioSesion();
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Sesión expirada"));
        }

        chatbotService.cerrarSesion(sessionUser.getIdUsuario());
        chatbotService.obtenerOCrearSesion(sessionUser.getIdUsuario());

        return ResponseEntity.ok(Map.of("mensaje", "Nueva conversación iniciada"));
    }
}

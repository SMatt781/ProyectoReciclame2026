---
name: reciclame-superadmin-realtime
description: Actualiza en vivo (WebSocket/STOMP) las métricas y acciones del panel Superadmin sin recargar la página, reutilizando la infraestructura STOMP que ya existe en el proyecto. Usar solo para archivos del rol SUPERADMIN.
---

# Superadmin — actualizaciones en tiempo real

## Contexto real del proyecto (verificado en código)

El proyecto YA tiene WebSocket/STOMP funcionando end-to-end. No hay que instalar
nada nuevo ni levantar infraestructura — solo extender un patrón que ya existe
y funciona:

- `Config/WebSocketConfig.java` — configura el endpoint STOMP/SockJS.
- `Service/NotificacionWebSocketService.java` — helper para publicar en topics
  (`enviarTopico(topic, payload)`) y notificaciones por usuario (`enviarNotificacion`).
- `Service/UsuariosActivosScheduler.java` — **ejemplo funcionando ahora mismo**:
  cada 30s (`@Scheduled(fixedDelay = 30000)`) cuenta sesiones activas y hace
  `webSocketService.enviarTopico("/topic/admin/conectados", Map.of("count", count))`.
- `fragments/navbar.html` (líneas ~291-345) — **ejemplo funcionando del lado
  cliente**: conecta con SockJS/Stomp y hace
  `stompClient.subscribe('/topic/admin/conectados', function(message) {...})`
  actualizando `#usuarios-conectados-count` sin reload.

Esto significa: el patrón "broadcast periódico → subscribe en el cliente →
reemplazar un nodo del DOM" ya está probado en este mismo repo. Solo hay que
copiarlo para las otras métricas.

## Alcance — SOLO archivos de SUPERADMIN

- Tocar libremente: `Controller/SuperadminController.java`,
  `Service/UsuariosActivosScheduler.java` (o un scheduler nuevo hermano),
  `templates/superadmin/*.html`, `fragments/sidebar_superadmin.html`.
- NO tocar: `AdminController`, `SocioController`, `VisualizadorController`,
  ni sus templates.
- NO cambiar clases de color (`bg-*`, `text-*`, `border-*`) ni la estructura
  de `<table>`/tarjetas existentes — solo agregar `id`, `data-*`, `aria-live`
  o JS que las actualice.
- **Única excepción real:** `fragments/navbar.html` es compartido por los 4
  roles. Si una mejora requiere tocarlo (ver caso "cierre de sesión instantáneo"
  abajo), avisar antes al equipo — es un cambio pequeño y aditivo (JS), no
  visual, pero afecta a todos los roles porque el navbar se carga en todas
  las páginas.

## Caso 1 — Tarjetas de `estadoSistema.html` no se actualizan solas

**Diagnóstico:** `SuperadminController.showEstadoSistema()` calcula CPU, RAM,
disco, sesiones activas, intentos fallidos y `logEntries` **una sola vez por
request**. A diferencia del contador de "usuarios conectados" (que sí tiene
un scheduler empujando datos), estas métricas no tienen ningún broadcast —
por eso solo cambian cuando el superadmin recarga la página manualmente.

**Solución (100% dentro de tus archivos):**
1. Extraer el bloque de cálculo de métricas de `showEstadoSistema()` a un
   método privado reutilizable, ej. `Map<String,Object> calcularMetricasSistema()`.
2. Crear un scheduler nuevo (o ampliar `UsuariosActivosScheduler`) que llame
   a ese método cada 15-30s y haga
   `webSocketService.enviarTopico("/topic/superadmin/estado", metricas)`.
3. En `estadoSistema.html`, agregar un `stompClient.subscribe('/topic/superadmin/estado', ...)`
   (copiando el bloque que ya existe en `navbar.html`) que actualice solo los
   `id` de las tarjetas de CPU/RAM/disco/sesiones/intentos, sin tocar el resto
   del DOM ni sus clases de color.
4. Los `logEntries` nuevos se agregan arriba de la lista existente (prepend),
   no se regenera toda la terminal — así el superadmin no pierde su scroll.

## Caso 2 — "Cerrar sesiones" no desconecta al instante

**Diagnóstico exacto:** el botón "Resetear sesiones" llama a
`POST /superadmin/sistema/resetear-sesiones` →
`SuperadminController.resetearSesiones()` (líneas ~1259-1266) →
`SessionStore.invalidarTodas()` (líneas ~30-38), que **sí invalida las
sesiones en el servidor de forma inmediata** (`session.invalidate()`). El
problema no es de seguridad, es de UX: al usuario afectado nadie le avisa —
su pestaña sigue mostrando la página anterior hasta que hace clic en algo
o recarga, y ahí recién Spring Security detecta la sesión inválida y lo
manda a `/login`.

**Solución:**
1. En `resetearSesiones()`, después de `invalidarTodas()`, publicar un
   broadcast: `webSocketService.enviarTopico("/topic/sistema/sesion-cerrada", Map.of("forzado", true))`.
   Esto es 100% código tuyo (`SuperadminController.java`).
2. **Única parte que toca código compartido:** como se están matando sesiones
   de ADMIN/SOCIO/VISUALIZADOR (no solo superadmin), el listener que redirige
   al usuario afectado debe vivir en `fragments/navbar.html` (donde ya existe
   la conexión STOMP activa para todos los roles) — agregar un
   `stompClient.subscribe('/topic/sistema/sesion-cerrada', () => window.location.href = '/login')`.
   Son ~5 líneas de JS junto al bloque STOMP que ya existe ahí, sin tocar
   nada visual. Avisar al equipo antes de tocar este archivo compartido.
3. Alternativa sin tocar `navbar.html`: dejar el comportamiento actual
   (invalidación real e inmediata en servidor, pero el usuario se entera
   recién en su próxima navegación). Es seguro, solo no es "instantáneo".

## Nota sobre el resto del repo

El `git log` remoto ya tiene commits de un compañero de equipo tipo
"notis en tiempo real listas" / "Mejorar de tiempo real en registro de
usuarios" que no están todavía en el local. Antes de implementar el Caso 1
o 2, revisar esos commits — puede que ya exista un patrón adicional de
broadcast reutilizable, o incluso que parte de esto ya esté resuelto para
otro rol y solo haya que replicarlo para superadmin.

# 📋 CORRECCIÓN: Mostrar DNI O RUC (No Ambos)

## ✅ PROBLEMA SOLUCIONADO

**Antes:** Las vistas mostraban tanto DNI como RUC siempre, incluso cuando solo se usaba uno

**Ahora:** Solo se muestra el documento que el usuario proporcionó (DNI O RUC)

---

## 🔧 Archivos Modificados

### 1. Vista de Detalle de Solicitud
**Archivo:** `src/main/resources/templates/admin/detalle-solicitud.html`

**Cambios:**

#### En el perfil del usuario (línea 149)
```html
<!-- ANTES: Siempre mostraba DNI -->
<p class="mt-2 text-sm text-on-surface-variant" th:text="|DNI: ${usuario.dni}|"></p>

<!-- AHORA: Muestra DNI O RUC según corresponda -->
<p class="mt-2 text-sm text-on-surface-variant" 
   th:if="${usuario.dni != null and !usuario.dni.isEmpty()}" 
   th:text="|DNI: ${usuario.dni}|"></p>
<p class="mt-2 text-sm text-on-surface-variant" 
   th:if="${solicitudExtra != null and solicitudExtra.ruc != null and !solicitudExtra.ruc.isEmpty()}" 
   th:text="|RUC: ${solicitudExtra.ruc}|"></p>
```

#### En los datos del solicitante (líneas 197-217)
```html
<!-- ANTES: Mostraba ambos campos, con "-" si estaba vacío -->
<div>
    <label>DNI</label>
    <div th:text="${usuario.dni}"></div>
</div>
<!-- Siempre presente -->
<div>
    <label>RUC</label>
    <div th:text="${solicitudExtra != null and solicitudExtra.ruc != null ? solicitudExtra.ruc : '-'}"></div>
</div>

<!-- AHORA: Solo muestra el que tiene valor -->
<div th:if="${usuario.dni != null and !usuario.dni.isEmpty()}">
    <label>DNI</label>
    <div th:text="${usuario.dni}"></div>
</div>

<div th:if="${solicitudExtra != null and solicitudExtra.ruc != null and !solicitudExtra.ruc.isEmpty()}">
    <label>RUC</label>
    <div th:text="${solicitudExtra.ruc}"></div>
</div>
```

### 2. Perfil de Usuario
**Archivo:** `src/main/resources/templates/perfilUsuario.html`

**Cambios:**

```html
<!-- ANTES: Mostraba DNI siempre -->
<div class="space-y-2">
    <label>DNI (No editable)</label>
    <input type="text" th:value="${usuario.dni}" readonly>
</div>

<!-- AHORA: Solo muestra si existe -->
<div class="space-y-2" th:if="${usuario.dni != null and !usuario.dni.isEmpty()}">
    <label>DNI (No editable)</label>
    <input type="text" th:value="${usuario.dni}" readonly>
</div>
```

---

## 🎯 Comportamiento Esperado

### Después de estas correcciones:

| Caso | DNI Visible | RUC Visible |
|------|---|---|
| Usuario se registró con DNI | ✅ Sí | ❌ No |
| Usuario se registró con RUC | ❌ No | ✅ Sí |
| Ambos campos vacíos | ❌ No | ❌ No |

---

## 📍 Vistas Afectadas

### Vista de Detalle de Solicitud
- ✅ Perfil lateral: muestra DNI O RUC
- ✅ Sección "Datos del solicitante": muestra DNI O RUC

### Perfil de Usuario
- ✅ Campo DNI: solo visible si existe

---

## 🧪 Cómo Verificar

1. **Crear un socio con DNI:**
   - Ir a `/admin/usuarios/solicitudes/{id}`
   - Debería ver: DNI (sí), RUC (no)

2. **Crear un socio con RUC:**
   - Ir a `/admin/usuarios/solicitudes/{id}`
   - Debería ver: DNI (no), RUC (sí)

3. **Ver perfil de usuario:**
   - Ir a `/perfil`
   - Si es socio con DNI: muestra DNI (no vacío)
   - Si es socio con RUC: no muestra campo DNI

---

## ✨ Resultado

Las vistas ahora son más limpias y lógicas, mostrando solo la información que el usuario proporcionó durante el registro.

**Status:** 🟢 **COMPLETADO**

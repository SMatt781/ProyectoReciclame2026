// =====================================================
// SPINNER.JS - Loading state garantizado en botones
// Usa preventDefault + submit diferido para que el
// spinner siempre sea visible aunque el servidor sea rápido.
// =====================================================

(function () {
    var MIN_VISIBLE_MS = 700; // mínimo de visibilidad en ms

    function spinnerHTML() {
        return [
            '<svg class="animate-spin w-4 h-4 inline-block mr-2 flex-shrink-0"',
            '     xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">',
            '  <circle class="opacity-25" cx="12" cy="12" r="10"',
            '          stroke="currentColor" stroke-width="4"></circle>',
            '  <path class="opacity-75" fill="currentColor"',
            '        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>',
            '</svg>',
            '<span>Procesando...</span>'
        ].join('');
    }

    function attachSpinner(form) {
        // Evitar registrar el evento dos veces en el mismo formulario
        if (form._spinnerAttached) return;
        form._spinnerAttached = true;

        form.addEventListener('submit', function (e) {
            var btn = form.querySelector('button[type="submit"]');
            if (!btn || btn.dataset.noSpinner === 'true') return;

            // ─── Clave: detener envío inmediato ───────────────
            e.preventDefault();

            var originalHTML = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = spinnerHTML();

            // Esperar MIN_VISIBLE_MS y luego enviar el formulario de verdad
            // form.submit() no dispara el evento 'submit', así evitamos bucle infinito
            setTimeout(function () {
                form.submit();
            }, MIN_VISIBLE_MS);
        });
    }

    function init() {
        document.querySelectorAll('form').forEach(attachSpinner);

        // Observar formularios añadidos dinámicamente (modales, etc.)
        if (window.MutationObserver) {
            var observer = new MutationObserver(function (mutations) {
                mutations.forEach(function (m) {
                    m.addedNodes.forEach(function (node) {
                        if (node.nodeType !== 1) return;
                        if (node.tagName === 'FORM') {
                            attachSpinner(node);
                        } else if (node.querySelectorAll) {
                            node.querySelectorAll('form').forEach(attachSpinner);
                        }
                    });
                });
            });
            observer.observe(document.body, { childList: true, subtree: true });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

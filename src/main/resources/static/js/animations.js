// =====================================================
// ANIMATIONS.JS - Recíclame Visual Enhancements
// =====================================================

// ─── 1. COUNTER ANIMATION ─────────────────────────
// Busca todos los elementos con clase .kpi-counter y anima desde 0 hasta el valor real.
// Soporta sufijos de texto: ej. "15 min" → anima el número y conserva " min"

function animateCounter(el, duration) {
    duration = duration || 1600;
    var raw = el.textContent.trim();

    // Extraer número y posible sufijo (ej. "15 min" → num=15, suffix=" min")
    var match = raw.match(/^(\d[\d.,]*)(.*)$/);
    if (!match) return;

    var target = parseInt(match[1].replace(/[.,]/g, ''), 10);
    var suffix = match[2] || '';   // ej. " min", " caracteres", ""

    if (isNaN(target) || target === 0) return;

    var start = performance.now();
    el.textContent = '0' + suffix;

    function update(now) {
        var elapsed = now - start;
        var progress = Math.min(elapsed / duration, 1);
        // easeOutCubic: arranca rápido y desacelera al final
        var eased = 1 - Math.pow(1 - progress, 3);
        var current = Math.floor(eased * target);
        el.textContent = current.toLocaleString('es-PE') + suffix;
        if (progress < 1) {
            requestAnimationFrame(update);
        } else {
            el.textContent = target.toLocaleString('es-PE') + suffix;
        }
    }

    requestAnimationFrame(update);
}

document.addEventListener('DOMContentLoaded', function () {
    var counters = document.querySelectorAll('.kpi-counter');
    if (!counters.length) return;

    if ('IntersectionObserver' in window) {
        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    animateCounter(entry.target);
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.4 });

        counters.forEach(function (el) { observer.observe(el); });
    } else {
        // Fallback para navegadores sin IntersectionObserver
        counters.forEach(function (el) { animateCounter(el); });
    }
});

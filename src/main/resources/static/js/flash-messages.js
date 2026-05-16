document.addEventListener('DOMContentLoaded', function() {
    // Detecta elementos con clase 'flash-message'
    const messages = document.querySelectorAll('.flash-message');

    messages.forEach(function(message) {
        if (message && message.offsetParent !== null) { // Verificar que sea visible
            // Esperar 5 segundos
            setTimeout(function() {
                // Agregar transición fade-out
                message.style.transition = 'opacity 0.5s ease-out';
                message.style.opacity = '0';

                // Remover del DOM después de la transición
                setTimeout(function() {
                    message.remove();
                }, 500); // 500ms para la animación
            }, 5000); // 5 segundos
        }
    });
});
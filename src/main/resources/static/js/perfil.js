/**
 * Script para manejo de foto de perfil
 * Incluye: drag-drop, vista previa, validación
 */

document.addEventListener('DOMContentLoaded', function() {
    const dropZone = document.getElementById('drop-zone');
    const inputFoto = document.getElementById('input-foto');
    const previewContainer = document.getElementById('preview-container');
    const previewFoto = document.getElementById('preview-foto');
    const previewNombre = document.getElementById('preview-nombre');
    const formFoto = document.getElementById('form-foto');

    // ===== DRAG AND DROP =====
    if (dropZone) {
        // Prevenir comportamiento por defecto
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, preventDefaults, false);
        });

        function preventDefaults(e) {
            e.preventDefault();
            e.stopPropagation();
        }

        // Highlight en drag
        ['dragenter', 'dragover'].forEach(eventName => {
            dropZone.addEventListener(eventName, highlight, false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, unhighlight, false);
        });

        function highlight(e) {
            dropZone.classList.add('border-emerald-400', 'bg-emerald-50');
        }

        function unhighlight(e) {
            dropZone.classList.remove('border-emerald-400', 'bg-emerald-50');
        }

        // Handle drop
        dropZone.addEventListener('drop', handleDrop, false);

        function handleDrop(e) {
            const dt = e.dataTransfer;
            const files = dt.files;

            if (files.length > 0) {
                inputFoto.files = files;
                mostrarPreview(files[0]);
            }
        }
    }

    // ===== CLICK PARA SELECCIONAR =====
    if (inputFoto) {
        inputFoto.addEventListener('change', function(e) {
            if (this.files.length > 0) {
                mostrarPreview(this.files[0]);
            }
        });
    }

    // ===== MOSTRAR PREVIEW =====
    function mostrarPreview(archivo) {
        // Validar tamaño
        const maxSize = 5 * 1024 * 1024; // 5MB
        if (archivo.size > maxSize) {
            alert('El archivo es demasiado grande. Máximo 5MB.');
            inputFoto.value = '';
            previewContainer.classList.add('hidden');
            return;
        }

        // Validar tipo
        const tiposPermitidos = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
        if (!tiposPermitidos.includes(archivo.type)) {
            alert('El archivo debe ser una imagen válida (JPG, PNG, GIF).');
            inputFoto.value = '';
            previewContainer.classList.add('hidden');
            return;
        }

        // Mostrar preview
        const reader = new FileReader();
        reader.onload = function(e) {
            previewFoto.src = e.target.result;
            previewNombre.textContent = archivo.name + ' - ' + (archivo.size / 1024).toFixed(2) + ' KB';
            previewContainer.classList.remove('hidden');
        };
        reader.readAsDataURL(archivo);
    }

    // ===== RESET FORM =====
    if (formFoto) {
        formFoto.addEventListener('reset', function() {
            previewContainer.classList.add('hidden');
            inputFoto.value = '';
        });
    }
});

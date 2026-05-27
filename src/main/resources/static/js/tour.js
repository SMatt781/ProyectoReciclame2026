/**
 * ReciclaTour — Tour guiado sin dependencias externas.
 * Usa localStorage para persistir el estado sin modificar la base de datos.
 *
 * Uso básico:
 *   window.reciclaTourInstance = new ReciclaTour({ usuarioId, rol, steps });
 *   window.reciclaTourInstance.startIfNew();
 */
(function (global) {
    'use strict';

    /* ─────────────────────────────────────────────────────────── */
    /*  CLASE PRINCIPAL                                            */
    /* ─────────────────────────────────────────────────────────── */
    class ReciclaTour {
        constructor(config) {
            this.usuarioId   = config.usuarioId || 0;
            this.rol         = (config.rol || 'SOCIO').toLowerCase();
            this.steps       = config.steps || [];
            this.storageKey  = 'reciclame_tour_' + this.rol + '_' + this.usuarioId;
            this.pendingKey  = 'reciclame_tour_pending_' + this.rol + '_' + this.usuarioId;
            this.currentStep = 0;
            this.isActive    = false;
            this._els        = {};
        }

        /* ── Estado ── */
        hasCompleted()   { return localStorage.getItem(this.storageKey) === 'true'; }
        markCompleted()  { localStorage.setItem(this.storageKey, 'true'); localStorage.removeItem(this.pendingKey); }
        reset()          { localStorage.removeItem(this.storageKey); }

        /**
         * Inicia el tour si el usuario nunca lo completó,
         * o si fue redirigido desde otra página con pending=true.
         */
        startIfNew() {
            const pending = localStorage.getItem(this.pendingKey) === 'true';
            if (pending || !this.hasCompleted()) {
                localStorage.removeItem(this.pendingKey);
                setTimeout(() => this.start(), 700);
            }
        }

        start() {
            if (this.isActive) return;
            this.currentStep = 0;
            this.isActive    = true;
            document.body.style.overflow = 'hidden';
            this._build();
            this._render(0);
        }

        restart() {
            if (this.isActive) this._destroy();
            this.reset();
            this.start();
        }

        /* ─────────────────────────────────────────────────────── */
        /*  CONSTRUCCIÓN DEL DOM                                   */
        /* ─────────────────────────────────────────────────────── */
        _build() {
            this._cleanup();

            /* Bloqueo de clicks */
            const blocker = document.createElement('div');
            blocker.id = 'rt-blocker';
            blocker.style.cssText = 'position:fixed;inset:0;z-index:9950;cursor:default;';
            document.body.appendChild(blocker);

            /* Overlay oscuro */
            const overlay = document.createElement('div');
            overlay.id = 'rt-overlay';
            overlay.style.cssText = [
                'position:fixed', 'inset:0', 'z-index:9951',
                'pointer-events:none', 'background:transparent',
                'transition:background 0.4s ease'
            ].join(';');
            document.body.appendChild(overlay);
            requestAnimationFrame(() => {
                overlay.style.background = 'rgba(2,29,48,0.72)';
            });

            /* Spotlight — hueco de luz mediante box-shadow masivo */
            const spot = document.createElement('div');
            spot.id = 'rt-spotlight';
            spot.style.cssText = [
                'position:fixed', 'z-index:9952', 'pointer-events:none',
                'border-radius:14px', 'background:transparent',
                'transition:all 0.45s cubic-bezier(0.4,0,0.2,1)',
                'box-shadow:0 0 0 9999px rgba(2,29,48,0.72)',
                'width:0', 'height:0', 'left:50%', 'top:50%'
            ].join(';');
            document.body.appendChild(spot);

            /* Tarjeta principal */
            const card = document.createElement('div');
            card.id = 'rt-card';
            card.style.cssText = [
                'position:fixed', 'z-index:9999',
                'width:340px', 'max-width:calc(100vw - 24px)',
                'background:#ffffff', 'border-radius:20px',
                'padding:22px 24px 20px',
                'box-shadow:0 24px 64px rgba(2,29,48,0.22),0 4px 20px rgba(2,29,48,0.10)',
                'font-family:Inter,system-ui,sans-serif',
                'opacity:0', 'transform:translateY(12px) scale(0.96)',
                'transition:opacity 0.38s ease,transform 0.38s ease'
            ].join(';');
            document.body.appendChild(card);

            /* Pequeña flecha / conector */
            const arrow = document.createElement('div');
            arrow.id = 'rt-arrow';
            arrow.style.cssText = [
                'position:fixed', 'z-index:9998',
                'width:12px', 'height:12px', 'background:#ffffff',
                'transform:rotate(45deg)', 'pointer-events:none',
                'display:none',
                'transition:left 0.45s cubic-bezier(0.4,0,0.2,1),top 0.45s cubic-bezier(0.4,0,0.2,1)'
            ].join(';');
            document.body.appendChild(arrow);

            this._els = { blocker, overlay, spot, card, arrow };

            /* Animar entrada de la tarjeta */
            requestAnimationFrame(() => {
                setTimeout(() => {
                    card.style.opacity   = '1';
                    card.style.transform = 'translateY(0) scale(1)';
                }, 60);
            });
        }

        /* ─────────────────────────────────────────────────────── */
        /*  RENDERIZADO DE UN PASO                                 */
        /* ─────────────────────────────────────────────────────── */
        _render(index) {
            const { card } = this._els;
            const step   = this.steps[index];
            const total  = this.steps.length;
            const isFirst = index === 0;
            const isLast  = index === total - 1;

            /* Spotlight sobre el elemento objetivo */
            let targetEl = null;
            if (step.target) {
                targetEl = document.querySelector(step.target);
            }
            this._updateSpotlight(targetEl);

            /* Puntos de progreso */
            const dots = this.steps.map((_, i) => {
                const active = (i === index);
                return '<div style="' +
                    'width:' + (active ? '18px' : '6px') + ';height:6px;border-radius:99px;' +
                    'background:' + (active ? '#2ECC71' : '#dde1e5') + ';' +
                    'transition:all 0.3s ease;flex-shrink:0;' +
                '"></div>';
            }).join('');

            /* HTML de la tarjeta */
            card.innerHTML =
                /* ── Cabecera ── */
                '<div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">' +
                    '<div style="display:flex;align-items:center;gap:8px;">' +
                        '<div style="width:28px;height:28px;border-radius:8px;' +
                            'background:linear-gradient(135deg,#2ECC71 0%,#006d37 100%);' +
                            'display:flex;align-items:center;justify-content:center;flex-shrink:0;">' +
                            '<svg width="15" height="15" viewBox="0 0 24 24" fill="white">' +
                                '<path d="M7 19c0 1.1.9 2 2 2h6c1.1 0 2-.9 2-2v-8h4V9h-4V7c0-1.1-.9-2-2-2h-4c-1.1 0-2 .9-2 2v2H3v2h4v8zm2-8h6V7H9v4z"/>' +
                            '</svg>' +
                        '</div>' +
                        '<span style="font-size:10px;font-weight:700;color:#73777d;text-transform:uppercase;letter-spacing:0.1em;">' +
                            'Tutorial &nbsp;' + (index + 1) + '&nbsp;/&nbsp;' + total +
                        '</span>' +
                    '</div>' +
                    (!isLast
                        ? '<button id="rt-skip" style="background:none;border:none;cursor:pointer;' +
                          'color:#a0a8b2;font-size:11px;font-weight:600;padding:4px 8px;border-radius:6px;' +
                          'transition:color 0.15s;" ' +
                          'onmouseover="this.style.color=\'#e63946\'" ' +
                          'onmouseout="this.style.color=\'#a0a8b2\'">Saltar ✕</button>'
                        : '<span></span>') +
                '</div>' +

                /* ── Emoji ── */
                (step.emoji
                    ? '<div style="font-size:34px;margin-bottom:10px;line-height:1;">' + step.emoji + '</div>'
                    : '') +

                /* ── Título y contenido ── */
                '<h3 style="font-family:\'Plus Jakarta Sans\',Inter,system-ui;font-weight:800;' +
                    'font-size:16px;color:#021d30;margin:0 0 7px;line-height:1.35;">' +
                    step.title +
                '</h3>' +
                '<p style="font-size:13px;color:#43474c;line-height:1.65;margin:0 0 18px;">' +
                    step.content +
                '</p>' +

                /* ── Puntos de progreso ── */
                '<div style="display:flex;align-items:center;justify-content:center;gap:5px;margin-bottom:18px;">' +
                    dots +
                '</div>' +

                /* ── Botones ── */
                '<div style="display:flex;gap:8px;">' +
                    (!isFirst
                        ? '<button id="rt-prev" style="flex:1;padding:9px 12px;border-radius:10px;' +
                          'border:1.5px solid #dde1e5;background:#fff;color:#43474c;' +
                          'font-size:13px;font-weight:600;cursor:pointer;transition:all 0.15s;" ' +
                          'onmouseover="this.style.borderColor=\'#2ECC71\';this.style.color=\'#006d37\'" ' +
                          'onmouseout="this.style.borderColor=\'#dde1e5\';this.style.color=\'#43474c\'">← Anterior</button>'
                        : '') +
                    '<button id="rt-next" style="flex:2;padding:9px 16px;border-radius:10px;' +
                        'border:none;background:linear-gradient(135deg,#2ECC71 0%,#00a651 100%);' +
                        'color:#fff;font-size:13px;font-weight:700;cursor:pointer;' +
                        'box-shadow:0 4px 14px rgba(46,204,113,0.35);transition:all 0.15s;" ' +
                        'onmouseover="this.style.transform=\'translateY(-1px)\';this.style.boxShadow=\'0 6px 18px rgba(46,204,113,0.45)\'" ' +
                        'onmouseout="this.style.transform=\'translateY(0)\';this.style.boxShadow=\'0 4px 14px rgba(46,204,113,0.35)\'">' +
                        (isLast ? '¡Entendido! 🎉' : 'Siguiente →') +
                    '</button>' +
                '</div>';

            /* ── Eventos ── */
            document.getElementById('rt-next')?.addEventListener('click', () => {
                if (isLast) this._complete();
                else this._goTo(index + 1);
            });
            document.getElementById('rt-prev')?.addEventListener('click', () => this._goTo(index - 1));
            document.getElementById('rt-skip')?.addEventListener('click', () => this._skip());

            /* ── Posición de la tarjeta ── */
            if (targetEl) {
                requestAnimationFrame(() => {
                    setTimeout(() => this._positionCard(targetEl, step.position || 'bottom'), 120);
                });
            } else {
                this._centerCard();
            }
        }

        /* ─────────────────────────────────────────────────────── */
        /*  SPOTLIGHT                                              */
        /* ─────────────────────────────────────────────────────── */
        _updateSpotlight(el) {
            const { spot } = this._els;
            if (!el) {
                spot.style.width      = '0px';
                spot.style.height     = '0px';
                spot.style.left       = '50%';
                spot.style.top        = '50%';
                spot.style.boxShadow  = 'none';
                return;
            }
            el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            requestAnimationFrame(() => {
                setTimeout(() => {
                    const r = el.getBoundingClientRect();
                    const pad = 8;
                    spot.style.left      = (r.left  - pad) + 'px';
                    spot.style.top       = (r.top   - pad) + 'px';
                    spot.style.width     = (r.width  + pad * 2) + 'px';
                    spot.style.height    = (r.height + pad * 2) + 'px';
                    spot.style.boxShadow = '0 0 0 9999px rgba(2,29,48,0.72)';
                }, 100);
            });
        }

        /* ─────────────────────────────────────────────────────── */
        /*  POSICIONAMIENTO DE LA TARJETA                          */
        /* ─────────────────────────────────────────────────────── */
        _positionCard(el, position) {
            const { card, arrow } = this._els;
            const r   = el.getBoundingClientRect();
            const cw  = 340;
            const ch  = card.offsetHeight || 300;
            const gap = 18;
            const vw  = window.innerWidth;
            const vh  = window.innerHeight;
            const margin = 10;

            let left, top, arrowL, arrowT, showArrow = true;

            switch (position) {
                case 'right':
                    left   = r.right + gap;
                    top    = r.top + r.height / 2 - ch / 2;
                    arrowL = left - 7;
                    arrowT = r.top + r.height / 2 - 6;
                    break;
                case 'left':
                    left   = r.left - cw - gap;
                    top    = r.top + r.height / 2 - ch / 2;
                    arrowL = r.left - gap + 1;
                    arrowT = r.top + r.height / 2 - 6;
                    break;
                case 'top':
                    left   = r.left + r.width / 2 - cw / 2;
                    top    = r.top - ch - gap;
                    arrowL = r.left + r.width / 2 - 6;
                    arrowT = r.top - gap + 1;
                    break;
                default: /* bottom */
                    left   = r.left + r.width / 2 - cw / 2;
                    top    = r.bottom + gap;
                    arrowL = r.left + r.width / 2 - 6;
                    arrowT = top - 7;
            }

            /* Ajuste al viewport */
            if (left + cw + margin > vw) left = vw - cw - margin;
            if (left < margin)           left = margin;
            if (top + ch + margin > vh)  top  = vh - ch - margin;
            if (top < margin)            top  = margin;

            card.style.left      = left + 'px';
            card.style.top       = top  + 'px';
            card.style.transform = 'none';

            /* Flecha */
            if (showArrow && arrowL !== undefined) {
                arrowL = Math.max(margin, Math.min(vw - 20, arrowL));
                arrowT = Math.max(margin, Math.min(vh - 20, arrowT));
                arrow.style.left    = arrowL + 'px';
                arrow.style.top     = arrowT + 'px';
                arrow.style.display = 'block';
            }
        }

        _centerCard() {
            const { card, arrow } = this._els;
            card.style.left      = '50%';
            card.style.top       = '50%';
            card.style.transform = 'translate(-50%,-50%)';
            arrow.style.display  = 'none';
        }

        /* ─────────────────────────────────────────────────────── */
        /*  TRANSICIÓN ENTRE PASOS                                 */
        /* ─────────────────────────────────────────────────────── */
        _goTo(index) {
            const { card } = this._els;
            card.style.opacity   = '0.55';
            card.style.transform = 'translateY(4px) scale(0.98)';
            setTimeout(() => {
                card.style.opacity   = '1';
                card.style.transform = 'none';
                this.currentStep = index;
                this._render(index);
            }, 170);
        }

        /* ─────────────────────────────────────────────────────── */
        /*  SALTAR                                                 */
        /* ─────────────────────────────────────────────────────── */
        _skip() {
            this.markCompleted();
            this._animateOut();
        }

        /* ─────────────────────────────────────────────────────── */
        /*  COMPLETAR                                              */
        /* ─────────────────────────────────────────────────────── */
        _complete() {
            const { card, spot, arrow } = this._els;

            card.innerHTML =
                '<div style="text-align:center;padding:8px 0 4px;">' +
                    '<div style="font-size:52px;margin-bottom:14px;">🎉</div>' +
                    '<h3 style="font-family:\'Plus Jakarta Sans\',Inter,system-ui;font-weight:800;' +
                        'font-size:20px;color:#021d30;margin:0 0 8px;">¡Todo listo!</h3>' +
                    '<p style="font-size:13.5px;color:#43474c;margin:0 0 22px;line-height:1.65;">' +
                        'Ya conoces las funciones principales de Reciclame.<br>' +
                        '<strong style="color:#006d37;">¡Empieza a explorar!</strong>' +
                    '</p>' +
                    '<button id="rt-finish" style="' +
                        'display:inline-flex;align-items:center;gap:8px;' +
                        'padding:12px 32px;border-radius:12px;border:none;' +
                        'background:linear-gradient(135deg,#2ECC71 0%,#006d37 100%);' +
                        'color:#fff;font-size:14px;font-weight:700;cursor:pointer;' +
                        'box-shadow:0 4px 16px rgba(46,204,113,0.4);transition:all 0.15s;" ' +
                        'onmouseover="this.style.transform=\'translateY(-2px)\';this.style.boxShadow=\'0 8px 24px rgba(46,204,113,0.5)\'" ' +
                        'onmouseout="this.style.transform=\'translateY(0)\';this.style.boxShadow=\'0 4px 16px rgba(46,204,113,0.4)\'">' +
                        'Comenzar →' +
                    '</button>' +
                '</div>';

            this._centerCard();
            spot.style.boxShadow = 'none';
            spot.style.width     = '0';
            spot.style.height    = '0';
            arrow.style.display  = 'none';

            document.getElementById('rt-finish')?.addEventListener('click', () => {
                this.markCompleted();
                this._animateOut();
            });
        }

        /* ─────────────────────────────────────────────────────── */
        /*  ANIMACIÓN DE SALIDA Y DESTRUCCIÓN                      */
        /* ─────────────────────────────────────────────────────── */
        _animateOut() {
            const { overlay, spot, card } = this._els;
            [overlay, spot, card].forEach(el => {
                if (!el) return;
                el.style.transition = 'opacity 0.38s ease, transform 0.38s ease';
                el.style.opacity    = '0';
            });
            if (card) card.style.transform = 'translateY(-8px) scale(0.95)';
            setTimeout(() => this._destroy(), 420);
        }

        _destroy() {
            document.body.style.overflow = '';
            ['rt-blocker','rt-overlay','rt-spotlight','rt-card','rt-arrow'].forEach(id => {
                document.getElementById(id)?.remove();
            });
            this._els   = {};
            this.isActive = false;
        }

        _cleanup() {
            this._destroy();
        }
    }

    /* ─────────────────────────────────────────────────────────── */
    /*  FUNCIÓN GLOBAL PARA EL BOTÓN DEL NAVBAR                   */
    /*  Llamada desde: onclick="iniciarTourDesdeNavbar(this)"      */
    /* ─────────────────────────────────────────────────────────── */
    global.iniciarTourDesdeNavbar = function (btn) {
        if (window.reciclaTourInstance) {
            /* Estamos en la página del panel — reiniciar directamente */
            window.reciclaTourInstance.restart();
        } else {
            /* Estamos en otra página — limpiar estado y redirigir al inicio */
            const rol        = (btn.dataset.userRol  || 'socio').toLowerCase();
            const userId     = btn.dataset.userId    || '0';
            const homeUrl    = btn.dataset.homeUrl   || '/';
            const storageKey = 'reciclame_tour_' + rol + '_' + userId;
            const pendingKey = 'reciclame_tour_pending_' + rol + '_' + userId;

            /* Borrar el "completado" para que el tour abra sí o sí al llegar */
            localStorage.removeItem(storageKey);
            localStorage.setItem(pendingKey, 'true');

            window.location.href = homeUrl;
        }
    };

    /* Exponer la clase globalmente */
    global.ReciclaTour = ReciclaTour;

})(window);

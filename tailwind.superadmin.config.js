// Config de Tailwind exclusivo para el rol SUPERADMIN.
// No toca tailwind.config.js (raiz, compartido por los demas roles).
// Paleta unificada a partir de la mayoria de las paginas superadmin
// (dashboard, estadoSistema, notificaciones); administradores y
// confSeguridad usaban tonos de verde ligeramente distintos y quedan
// alineados aqui a un solo valor por token.
module.exports = {
    darkMode: "class",
    content: [
        "./src/main/resources/templates/superadmin/**/*.html",
        "./src/main/resources/templates/fragments/sidebar_superadmin.html",
        "./src/main/resources/templates/fragments/navbar.html",
        "./src/main/resources/templates/fragments/chatbot-widget.html"
    ],
    theme: {
        extend: {
            colors: {
                "on-secondary": "#ffffff",
                "secondary-fixed": "#c3ecd7",
                "surface-container-high": "#dce9ff",
                "surface-container-lowest": "#ffffff",
                "surface-container": "#e5eeff",
                "on-primary-fixed-variant": "#3f465c",
                "on-background": "#0b1c30",
                "inverse-surface": "#213145",
                "on-tertiary-container": "#3980f4",
                "on-secondary-container": "#476c5b",
                "inverse-on-surface": "#eaf1ff",
                "on-error": "#ffffff",
                "surface-bright": "#f8f9ff",
                "on-tertiary": "#ffffff",
                "surface-tint": "#565e74",
                "background": "#f8f9ff",
                "surface-dim": "#cbdbf5",
                "on-tertiary-fixed-variant": "#004395",
                "tertiary-container": "#001a42",
                "on-error-container": "#93000a",
                "on-surface-variant": "#45464d",
                "primary-container": "#131b2e",
                "on-secondary-fixed": "#002115",
                "error": "#ba1a1a",
                "outline-variant": "#c6c6cd",
                "secondary-fixed-dim": "#a8cfbc",
                "on-primary-container": "#7c839b",
                "tertiary-fixed-dim": "#adc6ff",
                "primary": "#000000",
                "secondary": "#416656",
                "surface-container-highest": "#d3e4fe",
                "on-tertiary-fixed": "#001a42",
                "surface-variant": "#d3e4fe",
                "surface-container-low": "#eff4ff",
                "primary-fixed-dim": "#bec6e0",
                "on-primary-fixed": "#131b2e",
                "on-secondary-fixed-variant": "#294e3f",
                "error-container": "#ffdad6",
                "primary-fixed": "#dae2fd",
                "outline": "#76777d",
                "on-surface": "#0b1c30",
                "inverse-primary": "#bec6e0",
                "secondary-container": "#c3ecd7",
                "on-primary": "#ffffff",
                "tertiary-fixed": "#d8e2ff",
                "surface": "#f8f9ff",
                "tertiary": "#000000"
            },
            borderRadius: { "DEFAULT": "0.25rem", "lg": "0.5rem", "xl": "0.75rem", "full": "9999px" },
            fontFamily: {
                "sans": ["Plus Jakarta Sans", "sans-serif"],
                "mono": ["JetBrains Mono", "monospace"],
                "headline": ["Plus Jakarta Sans", "system-ui", "sans-serif"],
                "body": ["Inter", "system-ui", "sans-serif"],
                "label": ["Inter", "system-ui", "sans-serif"],
                "headline-md": ["Plus Jakarta Sans"],
                "display-lg": ["Plus Jakarta Sans"],
                "label-md": ["Plus Jakarta Sans"],
                "title-lg": ["Plus Jakarta Sans"],
                "body-md": ["Plus Jakarta Sans"],
                "body-sm": ["Plus Jakarta Sans"],
                "display-xl": ["Plus Jakarta Sans"]
            },
            fontSize: {
                "headline-md": ["24px", { "lineHeight": "32px", "fontWeight": "600" }],
                "display-lg": ["30px", { "lineHeight": "38px", "letterSpacing": "-0.02em", "fontWeight": "700" }],
                "label-md": ["12px", { "lineHeight": "16px", "letterSpacing": "0.05em", "fontWeight": "600" }],
                "title-lg": ["18px", { "lineHeight": "28px", "fontWeight": "600" }],
                "body-md": ["16px", { "lineHeight": "24px", "fontWeight": "400" }],
                "body-sm": ["14px", { "lineHeight": "20px", "fontWeight": "400" }],
                "display-xl": ["36px", { "lineHeight": "44px", "letterSpacing": "-0.02em", "fontWeight": "700" }]
            },
            spacing: {
                "stack-sm": "8px",
                "stack-md": "16px",
                "sidebar-width": "260px",
                "stack-lg": "32px",
                "gutter-md": "24px",
                "container-padding": "40px"
            }
        }
    },
    plugins: [require("@tailwindcss/forms")]
};

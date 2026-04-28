package com.example.proyectoreciclame.Controller;

import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/superadmin")
public class SuperadminController {

    final UsuarioRepository usuarioRepository;
    public SuperadminController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        // Puedes añadir algún dato aquí si quieres
        model.addAttribute("titulo", "Dashboard");
        model.addAttribute("currentSection", "superadmin-dashboard");
        return "superadmin/dashboard"; // Este es el nombre del archivo HTML de tu vista
    }

    // Nuevo método para mostrar administradores
    @GetMapping("/administradores")
    public String showAdministradores(
            Model model,
            @RequestParam(value = "texto", required = false) String texto,
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        List<Long> rolIds = Arrays.asList(1L, 2L);

        int size = 3;

        Page<Usuario> administradores;
        if (texto != null && !texto.isEmpty()) {
            administradores = usuarioRepository.buscarEnGestion(texto, PageRequest.of(page, size));
        } else {
            administradores = usuarioRepository.findByRol_IdInAndEliminadoEnIsNull(rolIds, PageRequest.of(page, size));
        }

        long totalAdmins = usuarioRepository.countByRol_IdInAndEliminadoEnIsNull(rolIds);
        long activeAdmins = usuarioRepository.countActiveAdminsByRole(rolIds);
        long blockedAdmins = usuarioRepository.countBlockedAdminsByRole(rolIds);

        Map<Long, String> ultimoAccesoTexto = administradores.getContent().stream()
                .collect(Collectors.toMap(
                        Usuario::getIdUsuario,
                        admin -> formatearUltimoAcceso(admin.getUltimoAcceso())
                ));

        model.addAttribute("ultimoAccesoTexto", ultimoAccesoTexto);

        model.addAttribute("titulo", "Administradores");
        model.addAttribute("currentSection", "superadmin-administradores");

        model.addAttribute("administradores", administradores);
        model.addAttribute("totalAdmins", totalAdmins);
        model.addAttribute("activeAdmins", activeAdmins);
        model.addAttribute("blockedAdmins", blockedAdmins);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", administradores.getTotalPages());
        model.addAttribute("hasPrevious", administradores.hasPrevious());
        model.addAttribute("hasNext", administradores.hasNext());
        model.addAttribute("texto", texto);



        return "superadmin/administradores";
    }

    private String formatearUltimoAcceso(LocalDateTime ultimoAcceso) {
        if (ultimoAcceso == null) {
            return "Sin registro";
        }

        Duration duracion = Duration.between(ultimoAcceso, LocalDateTime.now());

        long minutos = duracion.toMinutes();
        long horas = duracion.toHours();
        long dias = duracion.toDays();

        if (minutos < 1) {
            return "Hace unos segundos";
        } else if (minutos < 60) {
            return "Hace " + minutos + (minutos == 1 ? " minuto" : " minutos");
        } else if (horas < 24) {
            return "Hace " + horas + (horas == 1 ? " hora" : " horas");
        } else {
            return "Hace " + dias + (dias == 1 ? " día" : " días");
        }
    }



    // Nuevo método para el Estado del Monitor
    @GetMapping("/estadoSistema")
    public String showEstadoSistema(Model model) {
        model.addAttribute("titulo", "Estado del Sistema");
        model.addAttribute("currentSection", "superadmin-estado-sistema");
        // Aquí podrías agregar más lógica si necesitas información adicional
        return "superadmin/estadoSistema"; // Vista del estado del sistema
    }

    // Nuevo método para la Configuración de Seguridad
    @GetMapping("/confSeguridad")
    public String showConfSeguridad(Model model) {
        model.addAttribute("titulo", "Configuración de Seguridad");
        model.addAttribute("currentSection", "superadmin-conf-seguridad");
        // Lógica relacionada con la configuración de seguridad
        return "superadmin/confSeguridad"; // Vista de configuración de seguridad
    }

}

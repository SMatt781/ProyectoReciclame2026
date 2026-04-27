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

import java.util.Arrays;
import java.util.List;

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
        return "superadmin/dashboard"; // Este es el nombre del archivo HTML de tu vista
    }

    // Nuevo método para mostrar administradores
    @GetMapping("/administradores")
    public String showAdministradores(Model model, @RequestParam(value = "texto", required = false) String texto) {
        // ID de los roles que corresponde a "ADMIN" (id=2) y "SUPERADMIN" (id=1) en la base de datos
        List<Long> rolIds = Arrays.asList(1L, 2L); // 1 = SUPERADMIN, 2 = ADMIN

        // Si hay texto de búsqueda, realizar búsqueda con filtros
        Page<Usuario> administradores;
        if (texto != null && !texto.isEmpty()) {
            administradores = usuarioRepository.buscarEnGestion(texto, PageRequest.of(0, 10)); // Buscar por texto
        } else {
            administradores = usuarioRepository.findByRol_IdInAndEliminadoEnIsNull(rolIds, PageRequest.of(0, 10));
        }

        // Contar administradores registrados (usuarios con rol ADMIN o SUPERADMIN)
        long totalAdmins = usuarioRepository.countByRol_IdInAndEliminadoEnIsNull(rolIds);

        // Contar administradores activos (con estado de cuenta "ACTIVO")
        long activeAdmins = usuarioRepository.countActiveAdminsByRole(rolIds);

        // Contar administradores bloqueados (con estado de cuenta "BLOQUEADO")
        long blockedAdmins = usuarioRepository.countBlockedAdminsByRole(rolIds);

        // Pasar los datos al modelo para los cards
        model.addAttribute("titulo", "Administradores");
        model.addAttribute("administradores", administradores);
        model.addAttribute("totalAdmins", totalAdmins);
        model.addAttribute("activeAdmins", activeAdmins);
        model.addAttribute("blockedAdmins", blockedAdmins);

        return "superadmin/administradores"; // Vista de administradores
    }

    // Nuevo método para el Estado del Monitor
    @GetMapping("/estadoSistema")
    public String showEstadoSistema(Model model) {
        model.addAttribute("titulo", "Estado del Sistema");
        // Aquí podrías agregar más lógica si necesitas información adicional
        return "superadmin/estadoSistema"; // Vista del estado del sistema
    }

    // Nuevo método para la Configuración de Seguridad
    @GetMapping("/confSeguridad")
    public String showConfSeguridad(Model model) {
        model.addAttribute("titulo", "Configuración de Seguridad");
        // Lógica relacionada con la configuración de seguridad
        return "superadmin/confSeguridad"; // Vista de configuración de seguridad
    }

}

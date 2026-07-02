package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Dto.SessionUserDto;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {

    private final UsuarioRepository usuarioRepository;

    public AuthenticatedUserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public SessionUserDto obtenerUsuarioSesion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }

        Usuario usuario = usuarioRepository.findByCorreoWithRol(auth.getName()).orElse(null);

        if (usuario == null) {
            return null;
        }

        SessionUserDto dto = new SessionUserDto();
        dto.setIdUsuario(usuario.getIdUsuario());
        dto.setNombres(limpiarSufijosRuc(usuario.getNombres()));
        dto.setApellidoPaterno(usuario.getApellidoPaterno());
        dto.setApellidoMaterno(usuario.getApellidoMaterno());
        dto.setCorreo(usuario.getCorreo());
        dto.setRol(usuario.getRol() != null ? usuario.getRol().getNombre() : "");

        String i1 = usuario.getNombres() != null && !usuario.getNombres().isBlank()
                ? usuario.getNombres().substring(0, 1).toUpperCase()
                : "";

        String i2 = usuario.getApellidoPaterno() != null && !usuario.getApellidoPaterno().isBlank()
                ? usuario.getApellidoPaterno().substring(0, 1).toUpperCase()
                : "";

        dto.setIniciales(i1 + i2);
        dto.setUrlAvatar(usuario.getUrlAvatar());

        return dto;
    }

    private String limpiarSufijosRuc(String nombre) {
        if (nombre == null) return null;
        return nombre.replaceAll("\\s*—\\s*(ACTIVO|BAJA|SUSPENDIDO|NO HABIDO|HABIDO).*$", "").trim();
    }
}
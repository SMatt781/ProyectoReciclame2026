package com.example.proyectoreciclame.Security;

import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreoWithRol(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if (!"APROBADO".equalsIgnoreCase(usuario.getEstadoAprobacion())
                || !"ACTIVO".equalsIgnoreCase(usuario.getEstadoCuenta())) {
            throw new UsernameNotFoundException("Cuenta no habilitada");
        }

        String rol = "VISUALIZADOR";
        if (usuario.getRol() != null && usuario.getRol().getNombre() != null) {
            rol = usuario.getRol().getNombre().toUpperCase();
        }

        return new org.springframework.security.core.userdetails.User(
                usuario.getCorreo(),
                usuario.getContrasenaHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
        );
    }
}
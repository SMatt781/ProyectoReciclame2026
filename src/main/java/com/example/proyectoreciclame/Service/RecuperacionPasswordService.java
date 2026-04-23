package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Entity.RecuperacionPassword;
import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Repository.RecuperacionPasswordRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RecuperacionPasswordService {

    private final UsuarioRepository usuarioRepository;
    private final RecuperacionPasswordRepository recuperacionPasswordRepository;
    private final CorreoService correoService;
    private final BCryptPasswordEncoder passwordEncoder;

    public RecuperacionPasswordService(UsuarioRepository usuarioRepository,
                                       RecuperacionPasswordRepository recuperacionPasswordRepository,
                                       CorreoService correoService,
                                       BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.recuperacionPasswordRepository = recuperacionPasswordRepository;
        this.correoService = correoService;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean usuarioPuedeRecuperar(String correo) {
        Optional<Usuario> opt = usuarioRepository.findByCorreoAndEliminadoEnIsNull(correo);
        if (opt.isEmpty()) {
            return false;
        }

        Usuario u = opt.get();

        return "APROBADO".equalsIgnoreCase(u.getEstadoAprobacion())
                && "ACTIVO".equalsIgnoreCase(u.getEstadoCuenta());
    }

    @Transactional
    public void generarYEnviarCodigo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoAndEliminadoEnIsNull(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        recuperacionPasswordRepository.deleteByUsuario(usuario);

        RecuperacionPassword rp = new RecuperacionPassword();
        rp.setUsuario(usuario);
        rp.setCodigo(generarCodigo6Digitos());
        rp.setCreadoEn(LocalDateTime.now());
        rp.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        rp.setUsado(false);

        recuperacionPasswordRepository.save(rp);
        correoService.enviarCodigoRecuperacion(correo, rp.getCodigo());
    }

    public boolean validarCodigo(String correo, String codigo) {
        Optional<Usuario> optUsuario = usuarioRepository.findByCorreoAndEliminadoEnIsNull(correo);
        if (optUsuario.isEmpty()) {
            return false;
        }

        Usuario usuario = optUsuario.get();

        Optional<RecuperacionPassword> opt = recuperacionPasswordRepository
                .findTopByUsuarioAndCodigoAndUsadoFalseOrderByCreadoEnDesc(usuario, codigo);

        if (opt.isEmpty()) {
            return false;
        }

        RecuperacionPassword rp = opt.get();

        return rp.getFechaExpiracion().isAfter(LocalDateTime.now());
    }

    public boolean cambiarPassword(String correo, String codigo, String nuevaPassword) {
        Optional<Usuario> optUsuario = usuarioRepository.findByCorreoAndEliminadoEnIsNull(correo);
        if (optUsuario.isEmpty()) {
            return false;
        }

        Usuario usuario = optUsuario.get();

        Optional<RecuperacionPassword> opt = recuperacionPasswordRepository
                .findTopByUsuarioAndCodigoAndUsadoFalseOrderByCreadoEnDesc(usuario, codigo);

        if (opt.isEmpty()) {
            return false;
        }

        RecuperacionPassword rp = opt.get();

        if (rp.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            return false;
        }

        usuario.setContrasenaHash(passwordEncoder.encode(nuevaPassword));
        usuario.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(usuario);

        rp.setUsado(true);
        recuperacionPasswordRepository.save(rp);

        return true;
    }

    private String generarCodigo6Digitos() {
        SecureRandom random = new SecureRandom();
        int numero = 100000 + random.nextInt(900000);
        return String.valueOf(numero);
    }
}
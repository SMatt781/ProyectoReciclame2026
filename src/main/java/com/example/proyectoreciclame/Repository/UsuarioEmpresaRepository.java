package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.Usuario;
import com.example.proyectoreciclame.Entity.UsuarioEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long> {

    Optional<UsuarioEmpresa> findByUsuario_IdUsuario(Long idUsuario);
    Optional<UsuarioEmpresa> findByUsuario(Usuario usuario);
}
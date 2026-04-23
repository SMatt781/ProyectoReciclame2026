package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("""
        SELECT u
        FROM Usuario u
        LEFT JOIN FETCH u.rol
        WHERE u.correo = :correo
          AND u.eliminadoEn IS NULL
    """)
    Optional<Usuario> findByCorreoWithRol(@Param("correo") String correo);

    @Query("""
        SELECT u
        FROM Usuario u
        LEFT JOIN FETCH u.rol r
        LEFT JOIN FETCH u.usuarioEmpresa ue
        WHERE u.eliminadoEn IS NULL
        ORDER BY u.fechaRegistro DESC
    """)
    Page<Usuario> findAllGestionUsuarios(Pageable pageable);

    @Query("""
        SELECT u
        FROM Usuario u
        LEFT JOIN FETCH u.rol r
        LEFT JOIN FETCH u.usuarioEmpresa ue
        WHERE u.eliminadoEn IS NULL
          AND (
            LOWER(u.nombres) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(u.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(COALESCE(u.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(u.dni) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(COALESCE(ue.razonSocial, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%'))
          )
        ORDER BY u.fechaRegistro DESC
    """)
    Page<Usuario> buscarEnGestion(@Param("texto") String texto, Pageable pageable);

    long countByEliminadoEnIsNull();

    long countByEstadoCuentaAndEliminadoEnIsNull(String estadoCuenta);

    long countByEstadoAprobacionAndEliminadoEnIsNull(String estadoAprobacion);

    Page<Usuario> findByEstadoAprobacionAndEliminadoEnIsNullOrderByFechaRegistroDesc(
            String estadoAprobacion, Pageable pageable);

    Optional<Usuario> findByCorreoAndEliminadoEnIsNull(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByDni(String dni);

    @Query("""
        SELECT u
        FROM Usuario u
        WHERE u.rol.id IN :rolIds
          AND u.eliminadoEn IS NULL
        ORDER BY u.fechaRegistro DESC
    """)
    Page<Usuario> findByRol_IdInAndEliminadoEnIsNull(List<Long> rolIds, Pageable pageable);

    @Query("""
        SELECT COUNT(u)
        FROM Usuario u
        WHERE u.rol.id IN :rolIds
          AND u.eliminadoEn IS NULL
    """)
    long countByRol_IdInAndEliminadoEnIsNull(List<Long> rolIds);

    @Query("""
    SELECT COUNT(u)
    FROM Usuario u
    LEFT JOIN u.rol r
    WHERE r.id IN :rolIds
      AND u.estadoCuenta = 'ACTIVO'
      AND u.eliminadoEn IS NULL
""")
    long countActiveAdminsByRole(List<Long> rolIds);

    @Query("""
    SELECT COUNT(u)
    FROM Usuario u
    LEFT JOIN u.rol r
    WHERE r.id IN :rolIds
      AND u.estadoCuenta = 'BLOQUEADO'
      AND u.eliminadoEn IS NULL
""")
    long countBlockedAdminsByRole(List<Long> rolIds);


}
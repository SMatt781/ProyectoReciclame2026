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

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    @Query("""
    SELECT u
    FROM Usuario u
    LEFT JOIN FETCH u.rol
    WHERE LOWER(u.correo) = LOWER(:correo)
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

    // ── Para AdminUsuarioController (todos los roles, con razonSocial) ──────────
    @Query("""
    SELECT u FROM Usuario u
    LEFT JOIN FETCH u.rol r
    LEFT JOIN FETCH u.usuarioEmpresa ue
    WHERE u.eliminadoEn IS NULL
      AND (
        :texto IS NULL OR :texto = ''
        OR LOWER(u.nombres) LIKE LOWER(CONCAT('%', :texto, '%'))
        OR LOWER(u.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%'))
        OR LOWER(COALESCE(u.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
        OR LOWER(u.dni) LIKE LOWER(CONCAT('%', :texto, '%'))
        OR LOWER(COALESCE(ue.razonSocial, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
        OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%'))
      )
      AND (
        :hasRoles = false OR UPPER(r.nombre) IN :roles
      )
      AND (
        :fechaInicio IS NULL OR u.fechaRegistro >= :fechaInicio
      )
      AND (
        :fechaFin IS NULL OR u.fechaRegistro <= :fechaFin
      )
      AND (
        :hasEstados = false
        OR ('Activo' IN :estados AND u.estadoCuenta = 'ACTIVO')
        OR ('Bloqueado' IN :estados AND u.estadoCuenta = 'BLOQUEADO')
        OR ('Pendiente' IN :estados AND u.estadoAprobacion = 'PENDIENTE')
        OR ('Rechazado' IN :estados AND u.estadoAprobacion = 'RECHAZADO')
      )
    ORDER BY u.fechaRegistro DESC
""")
    Page<Usuario> buscarEnGestionAvanzado(
            @Param("texto") String texto,
            @Param("hasRoles") boolean hasRoles,
            @Param("roles") List<String> roles,
            @Param("fechaInicio") java.time.LocalDateTime fechaInicio,
            @Param("fechaFin") java.time.LocalDateTime fechaFin,
            @Param("hasEstados") boolean hasEstados,
            @Param("estados") List<String> estados,
            Pageable pageable
    );

    // ── Para SuperadminController (filtrado por rol) ─────────────────────────────
    @Query("""
    SELECT u FROM Usuario u
    LEFT JOIN u.rol r
    WHERE u.eliminadoEn IS NULL
      AND r.idRol IN :rolIds
      AND (
        LOWER(u.nombres) LIKE LOWER(CONCAT('%', :texto, '%'))
        OR LOWER(u.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%'))
        OR LOWER(COALESCE(u.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
        OR LOWER(u.dni) LIKE LOWER(CONCAT('%', :texto, '%'))
        OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%'))
      )
""")
    Page<Usuario> buscarEnGestion(@Param("texto") String texto,
                                  @Param("rolIds") List<Integer> rolIds,
                                  Pageable pageable);

    long countByEliminadoEnIsNull();

    long countByEstadoCuentaAndEliminadoEnIsNull(Usuario.EstadoCuenta estadoCuenta);

    long countByEstadoAprobacionAndEliminadoEnIsNull(String estadoAprobacion);

    Page<Usuario> findByEstadoAprobacionAndEliminadoEnIsNullOrderByFechaRegistroDesc(
            String estadoAprobacion, Pageable pageable);

    Optional<Usuario> findByCorreoAndEliminadoEnIsNull(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByDni(String dni);


    @Query("""
    SELECT u
    FROM Usuario u
    WHERE u.rol.idRol IN :rolIds
      AND u.eliminadoEn IS NULL
    ORDER BY u.fechaRegistro DESC
""")
    Page<Usuario> findByRol_IdInAndEliminadoEnIsNull(List<Integer> rolIds, Pageable pageable);

    @Query("""
    SELECT COUNT(u)
    FROM Usuario u
    WHERE u.rol.idRol IN :rolIds
      AND u.eliminadoEn IS NULL
""")
    long countByRol_IdInAndEliminadoEnIsNull(List<Integer> rolIds);

    @Query("""
    SELECT COUNT(u)
    FROM Usuario u
    LEFT JOIN u.rol r
    WHERE r.idRol IN :rolIds
      AND u.estadoCuenta = 'ACTIVO'
      AND u.eliminadoEn IS NULL
""")
    long countActiveAdminsByRole(@Param("rolIds") List<Integer> rolIds);

    @Query("""
    SELECT COUNT(u)
    FROM Usuario u
    LEFT JOIN u.rol r
    WHERE r.idRol IN :rolIds
      AND u.estadoCuenta = 'BLOQUEADO'
      AND u.eliminadoEn IS NULL
""")
    long countBlockedAdminsByRole(@Param("rolIds") List<Integer> rolIds);
    boolean existsByCorreoAndEliminadoEnIsNull(String correo);
    boolean existsByDniAndEliminadoEnIsNull(String dni);

    @Query("""
    SELECT u
    FROM Usuario u
    LEFT JOIN FETCH u.rol r
    LEFT JOIN FETCH u.usuarioEmpresa ue
    WHERE u.estadoAprobacion = 'PENDIENTE'
      AND u.eliminadoEn IS NULL
      AND (
        :search IS NULL OR :search = ''
        OR LOWER(u.nombres) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.apellidoPaterno) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(COALESCE(u.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.dni) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(COALESCE(ue.razonSocial, '')) LIKE LOWER(CONCAT('%', :search, '%'))
      )
      AND (
        :rol IS NULL OR :rol = ''
        OR LOWER(r.nombre) = LOWER(:rol)
      )
      AND (
        :fechaInicio IS NULL OR u.fechaRegistro >= :fechaInicio
      )
      AND (
        :fechaFin IS NULL OR u.fechaRegistro <= :fechaFin
      )
    ORDER BY u.fechaRegistro DESC
""")
    Page<Usuario> filtrarSolicitudesPendientes(
            @Param("search") String search,
            @Param("rol") String rol,
            @Param("fechaInicio") java.time.LocalDateTime fechaInicio,
            @Param("fechaFin") java.time.LocalDateTime fechaFin,
            Pageable pageable
    );

    @Query("""
    SELECT COUNT(u)
    FROM Usuario u
    WHERE u.estadoCuenta = 'ACTIVO'
      AND u.estadoAprobacion = 'APROBADO'
      AND u.eliminadoEn IS NULL
""")
    long countUsuariosActivosAprobados();

    @Query("""
    SELECT COUNT(ue)
    FROM UsuarioEmpresa ue
    WHERE ue.usuario.eliminadoEn IS NULL
      AND ue.usuario.estadoAprobacion = 'APROBADO'
""")
    long countEmpresasRegistradas();

    // Para filtrar por estado + rol (sin texto)
    @Query("SELECT u FROM Usuario u WHERE u.rol.idRol IN :rolIds " +
            "AND u.eliminadoEn IS NULL " +
            "AND u.estadoCuenta = :estado")
    Page<Usuario> findByRolIdInAndEstadoCuentaAndEliminadoEnIsNull(
            @Param("rolIds") List<Integer> rolIds,
            @Param("estado") Usuario.EstadoCuenta estado,
            Pageable pageable);

    // Para filtrar por texto + estado + rol (combinado)
    @Query("SELECT u FROM Usuario u WHERE u.rol.idRol IN :rolIds " +
            "AND u.eliminadoEn IS NULL " +
            "AND u.estadoCuenta = :estado " +
            "AND (LOWER(u.nombres) LIKE LOWER(CONCAT('%',:texto,'%')) " +
            "OR LOWER(u.correo) LIKE LOWER(CONCAT('%',:texto,'%')))")
    Page<Usuario> buscarEnGestionConEstado(
            @Param("texto") String texto,
            @Param("rolIds") List<Integer> rolIds,
            @Param("estado") Usuario.EstadoCuenta estado,
            Pageable pageable);
}
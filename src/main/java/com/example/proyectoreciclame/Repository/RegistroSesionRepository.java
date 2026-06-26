package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.RegistroSesion;
import com.example.proyectoreciclame.Entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RegistroSesionRepository extends JpaRepository<RegistroSesion, Long> {

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    @Query(value = """
            SELECT rs
            FROM RegistroSesion rs
            JOIN rs.usuario u
            JOIN u.rol r
            WHERE u.eliminadoEn IS NULL
              AND UPPER(r.nombre) NOT IN ('ADMIN', 'SUPERADMIN')
              AND (:texto IS NULL OR
                   LOWER(u.nombres) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(u.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(COALESCE(u.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:fechaInicio IS NULL OR rs.fechaInicio >= :fechaInicio)
              AND (:fechaFin IS NULL OR rs.fechaInicio <= :fechaFin)
              AND (:rol IS NULL OR UPPER(rs.rolNombre) = UPPER(:rol))
            ORDER BY rs.fechaInicio DESC
            """,
            countQuery = """
            SELECT COUNT(rs)
            FROM RegistroSesion rs
            JOIN rs.usuario u
            JOIN u.rol r
            WHERE u.eliminadoEn IS NULL
              AND UPPER(r.nombre) NOT IN ('ADMIN', 'SUPERADMIN')
              AND (:texto IS NULL OR
                   LOWER(u.nombres) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(u.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(COALESCE(u.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:fechaInicio IS NULL OR rs.fechaInicio >= :fechaInicio)
              AND (:fechaFin IS NULL OR rs.fechaInicio <= :fechaFin)
              AND (:rol IS NULL OR UPPER(rs.rolNombre) = UPPER(:rol))
            """)
    Page<RegistroSesion> buscarFiltrado(@Param("texto") String texto,
                                        @Param("fechaInicio") LocalDateTime fechaInicio,
                                        @Param("fechaFin") LocalDateTime fechaFin,
                                        @Param("rol") String rol,
                                        Pageable pageable);

    Optional<RegistroSesion> findTopByUsuarioAndEstadoOrderByFechaInicioDesc(Usuario usuario, String estado);

    @Query("SELECT COUNT(rs) FROM RegistroSesion rs WHERE rs.usuario.idUsuario = :idUsuario AND rs.fechaInicio >= :fechaInicio AND rs.fechaInicio <= :fechaFin")
    long countByUsuarioIdUsuarioAndFechaInicioBetween(@Param("idUsuario") Long idUsuario,
                                                      @Param("fechaInicio") LocalDateTime fechaInicio,
                                                      @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT COUNT(DISTINCT FUNCTION('date', rs.fechaInicio)) FROM RegistroSesion rs WHERE rs.usuario.idUsuario = :idUsuario AND rs.fechaInicio >= :fechaInicio AND rs.fechaInicio <= :fechaFin")
    long countActiveDaysByUsuarioIdUsuarioBetween(@Param("idUsuario") Long idUsuario,
                                                  @Param("fechaInicio") LocalDateTime fechaInicio,
                                                  @Param("fechaFin") LocalDateTime fechaFin);
    List<RegistroSesion> findByUsuarioAndEstado(Usuario usuario, String estado);

    // 1. Promedio de duración (en minutos) — excluye admins/superadmins
    @Query("SELECT AVG(rs.duracionMinutos) FROM RegistroSesion rs JOIN rs.usuario u JOIN u.rol r WHERE rs.duracionMinutos IS NOT NULL AND UPPER(r.nombre) NOT IN ('ADMIN', 'SUPERADMIN')")
    Double getPromedioDuracion();

    // 2. Fecha de la última sesión — excluye admins/superadmins
    @Query("SELECT MAX(rs.fechaInicio) FROM RegistroSesion rs JOIN rs.usuario u JOIN u.rol r WHERE UPPER(r.nombre) NOT IN ('ADMIN', 'SUPERADMIN')")
    LocalDateTime getUltimaSesion();

    // 3. Total de usuarios activos (con sesión 'VIGENTE') — excluye admins/superadmins
    @Query("SELECT COUNT(DISTINCT rs.usuario.idUsuario) FROM RegistroSesion rs JOIN rs.usuario u JOIN u.rol r WHERE rs.estado = 'VIGENTE' AND UPPER(r.nombre) NOT IN ('ADMIN', 'SUPERADMIN')")
    long countUsuariosActivos();

    // 4. Nombre del usuario más frecuente — devuelve null si hay error
    default String getUsuarioMasFrecuente() {
        return null;
    }

    @Query("SELECT COUNT(r) FROM RegistroSesion r WHERE r.estado = 'VIGENTE' AND r.fechaInicio >= :desde")
    long countSesionesActivas(@Param("desde") java.time.LocalDateTime desde);

    long countByFechaInicioAfter(LocalDateTime fecha);

    // Usuarios distintos que tuvieron sesión en los últimos N días
    @Query(value = "SELECT COUNT(DISTINCT id_usuario) FROM registro_sesiones WHERE fecha_inicio >= :desde", nativeQuery = true)
    Long countUsuariosActivosDesde(@Param("desde") LocalDateTime desde);

    // Sesiones agrupadas por día para el gráfico de barras
    @Query(value = "SELECT DATE(fecha_inicio) AS dia, COUNT(*) AS total FROM registro_sesiones WHERE fecha_inicio >= :desde GROUP BY DATE(fecha_inicio) ORDER BY dia ASC", nativeQuery = true)
    List<Object[]> contarSesionesPorDia(LocalDateTime desde);

    List<RegistroSesion> findTop2ByUsuarioIdUsuarioOrderByFechaInicioDesc(Long idUsuario);

    // Marcar como EXPIRADAS todas las sesiones VIGENTE que sean más antiguas que cierta fecha
    @Modifying
    @Query("UPDATE RegistroSesion r SET r.estado = 'EXPIRADA' WHERE r.estado = 'VIGENTE' AND r.fechaInicio < :antes")
    int expirarSesionesAntiguas(@Param("antes") java.time.LocalDateTime antes);
}
package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Repository.IntentoLoginRepository;
import com.example.proyectoreciclame.Repository.RecuperacionPasswordRepository;
import com.example.proyectoreciclame.Repository.RegistroSesionRepository;
import com.example.proyectoreciclame.Repository.RolRepository;
import com.example.proyectoreciclame.Repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EstadoSistemaService {

    private final RegistroSesionRepository registroSesionRepository;
    private final IntentoLoginRepository intentoLoginRepository;
    private final RecuperacionPasswordRepository recuperacionPasswordRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public EstadoSistemaService(RegistroSesionRepository registroSesionRepository,
                                IntentoLoginRepository intentoLoginRepository,
                                RecuperacionPasswordRepository recuperacionPasswordRepository,
                                UsuarioRepository usuarioRepository,
                                RolRepository rolRepository) {
        this.registroSesionRepository = registroSesionRepository;
        this.intentoLoginRepository = intentoLoginRepository;
        this.recuperacionPasswordRepository = recuperacionPasswordRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    /**
     * Snapshot de las metricas dinamicas de superadmin/estadoSistema.html
     * (CPU, RAM, disco, latencia BD, sesiones, IA). Usado tanto en la
     * carga de pagina como en el broadcast periodico por WebSocket.
     */
    public Map<String, Object> calcularEstadoSistema() {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();

        long sesionesActivas = registroSesionRepository.countSesionesActivas(LocalDateTime.now().minusHours(8));
        long sesionesHoy = registroSesionRepository.countByFechaInicioAfter(inicioDia);
        long intentosFallidos = intentoLoginRepository.countByFechaAfterAndExitosoFalse(inicioDia);
        long recuperacionesActivas = recuperacionPasswordRepository.countByUsadoFalse();
        long solicitudesPendientes = usuarioRepository.countByEstadoAprobacionAndEliminadoEnIsNull("PENDIENTE");

        long t0 = System.currentTimeMillis();
        rolRepository.count();
        long latencia = System.currentTimeMillis() - t0;

        int cpuUsage = -1;
        int ramUsagePct = -1;
        int diskUsagePct = -1;
        long ramUsadaMb = 0;
        long ramTotalMb = 0;
        long usadoGb = 0;
        long totalGb = 0;

        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean)
                            java.lang.management.ManagementFactory.getOperatingSystemMXBean();

            double cpuLoad = osBean.getCpuLoad();
            cpuUsage = cpuLoad >= 0 ? (int) Math.round(cpuLoad * 100) : -1;

            Runtime runtime = Runtime.getRuntime();
            ramTotalMb = runtime.totalMemory() / (1024 * 1024);
            long ramLibreMb = runtime.freeMemory() / (1024 * 1024);
            ramUsadaMb = ramTotalMb - ramLibreMb;
            ramUsagePct = ramTotalMb > 0 ? (int) ((ramUsadaMb * 100) / ramTotalMb) : 0;

            java.io.File disco = new java.io.File("/");
            totalGb = disco.getTotalSpace() / (1024 * 1024 * 1024);
            long libreGb = disco.getUsableSpace() / (1024 * 1024 * 1024);
            usadoGb = totalGb - libreGb;
            diskUsagePct = totalGb > 0 ? (int) ((usadoGb * 100) / totalGb) : 0;
        } catch (Exception ignored) {
            cpuUsage = -1;
            ramUsagePct = -1;
            diskUsagePct = -1;
        }

        int puntosRiesgo = 0;
        List<String> factoresDetectados = new ArrayList<>();
        if (cpuUsage >= 80) {
            puntosRiesgo += 2;
            factoresDetectados.add("uso elevado de CPU");
        }
        if (ramUsagePct >= 80) {
            puntosRiesgo += 2;
            factoresDetectados.add("uso elevado de memoria RAM");
        }
        if (diskUsagePct >= 75) {
            puntosRiesgo += 2;
            factoresDetectados.add("almacenamiento cercano al límite");
        }
        if (latencia >= 100) {
            puntosRiesgo += 1;
            factoresDetectados.add("latencia elevada en la base de datos");
        }
        if (intentosFallidos >= 5) {
            puntosRiesgo += 2;
            factoresDetectados.add("múltiples intentos fallidos de acceso");
        }

        String iaNivelRiesgo;
        String iaResumenSistema;
        String iaRecomendacionSistema;
        if (puntosRiesgo >= 5) {
            iaNivelRiesgo = "CRÍTICO";
            iaResumenSistema = "La IA detectó señales relevantes de riesgo operativo o de seguridad en el sistema.";
            iaRecomendacionSistema = "Revisar recursos del servidor, limpiar almacenamiento, validar logs de acceso y verificar posibles intentos de ingreso no autorizados.";
        } else if (puntosRiesgo >= 3) {
            iaNivelRiesgo = "MEDIO";
            iaResumenSistema = "La IA detectó condiciones que podrían afectar el rendimiento si continúan aumentando.";
            iaRecomendacionSistema = "Monitorear CPU, RAM, disco, latencia de base de datos y actividad reciente de usuarios.";
        } else {
            iaNivelRiesgo = "BAJO";
            iaResumenSistema = "La IA no detectó riesgos relevantes. El sistema se encuentra estable.";
            iaRecomendacionSistema = "Mantener el monitoreo preventivo y revisar periódicamente las métricas del sistema.";
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sesionesActivas", sesionesActivas);
        data.put("sesionesHoy", sesionesHoy);
        data.put("intentosFallidosHoy", intentosFallidos);
        data.put("recuperacionesActivas", recuperacionesActivas);
        data.put("solicitudesPendientes", solicitudesPendientes);
        data.put("dbLatencyMs", latencia);
        data.put("cpuUsage", cpuUsage);
        data.put("ramUsage", ramUsagePct);
        data.put("ramUsadaMb", ramUsadaMb);
        data.put("ramTotalMb", ramTotalMb);
        data.put("diskUsage", diskUsagePct);
        data.put("diskUsadoGb", usadoGb);
        data.put("diskTotalGb", totalGb);
        data.put("iaNivelRiesgo", iaNivelRiesgo);
        data.put("iaResumenSistema", iaResumenSistema);
        data.put("iaRecomendacionSistema", iaRecomendacionSistema);
        data.put("iaFactoresDetectados", factoresDetectados);
        return data;
    }
}

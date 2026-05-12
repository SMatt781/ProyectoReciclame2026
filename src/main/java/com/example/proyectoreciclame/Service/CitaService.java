package com.example.proyectoreciclame.Service;

import com.example.proyectoreciclame.Entity.Estudio;
import com.example.proyectoreciclame.Entity.Normativa;
import com.example.proyectoreciclame.Repository.EstudioRepository;
import com.example.proyectoreciclame.Repository.NormativaRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CitaService {

    private final EstudioRepository estudioRepository;
    private final NormativaRepository normativaRepository;

    public CitaService(EstudioRepository estudioRepository, NormativaRepository normativaRepository) {
        this.estudioRepository = estudioRepository;
        this.normativaRepository = normativaRepository;
    }

    public Map<String, String> generarCitasEstudio(Long idEstudio) {
        Optional<Estudio> opt = estudioRepository.findById(idEstudio);
        if (opt.isEmpty()) return Map.of();

        Estudio e = opt.get();
        String titulo = e.getTitulo() != null ? e.getTitulo() : "Sin título";
        int anio = e.getAnio() != null ? e.getAnio() : 0;
        String publisher = "Reciclame – Repositorio Circular";
        String url = e.getArchivoUrl() != null ? e.getArchivoUrl() : "https://reciclame.pe";
        String anioStr = anio > 0 ? String.valueOf(anio) : "s.f.";

        Map<String, String> citas = new LinkedHashMap<>();

        citas.put("APA",
                publisher + ". (" + anioStr + "). " +
                "*" + titulo + "*. " + publisher + ". " + url);

        citas.put("MLA",
                publisher + ". \"" + titulo + ".\" " +
                publisher + ", " + anioStr + ". " + url);

        citas.put("Chicago",
                publisher + ". " + anioStr + ". " +
                "\"" + titulo + ".\" " + publisher + ". Accedido en " + url);

        citas.put("Harvard",
                publisher + " (" + anioStr + ") " +
                "*" + titulo + "*. " + publisher + ". Disponible en: " + url);

        return citas;
    }

    public Map<String, String> generarCitasNormativa(Long idNormativa) {
        Optional<Normativa> opt = normativaRepository.findById(idNormativa);
        if (opt.isEmpty()) return Map.of();

        Normativa n = opt.get();
        String titulo = n.getTitulo() != null ? n.getTitulo() : "Sin título";
        String organismo = n.getOrganismoEmisor() != null ? n.getOrganismoEmisor() : "Organismo emisor";
        String codigo = n.getCodigo() != null ? " (" + n.getCodigo() + ")" : "";
        int anio = n.getAnio() != null ? n.getAnio() : 0;
        String anioStr = anio > 0 ? String.valueOf(anio) : "s.f.";
        String enlace = n.getEnlaceExterno() != null ? n.getEnlaceExterno()
                : (n.getArchivoUrl() != null ? n.getArchivoUrl() : "");
        String urlParte = !enlace.isEmpty() ? " " + enlace : "";

        Map<String, String> citas = new LinkedHashMap<>();

        citas.put("APA",
                organismo + ". (" + anioStr + "). " +
                "*" + titulo + "*" + codigo + "." + urlParte);

        citas.put("MLA",
                organismo + ". \"" + titulo + ".\"" +
                codigo + " " + anioStr + "." + urlParte);

        citas.put("Chicago",
                organismo + ". " + anioStr + ". " +
                "\"" + titulo + "\"" + codigo + "." + urlParte);

        citas.put("Harvard",
                organismo + " (" + anioStr + ") " +
                "*" + titulo + "*" + codigo + "." +
                (!urlParte.isEmpty() ? " Disponible en:" + urlParte : ""));

        return citas;
    }
}

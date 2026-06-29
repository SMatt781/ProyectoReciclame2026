package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    List<Categoria> findByTipoAndEstadoTrue(Categoria.TipoCategoria tipo);

    Optional<Categoria> findByNombreIgnoreCaseAndTipo(String nombre, Categoria.TipoCategoria tipo);
}

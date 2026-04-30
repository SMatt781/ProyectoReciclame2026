package com.example.proyectoreciclame.Repository;

import com.example.proyectoreciclame.Entity.PoliticaContrasena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PoliticaContrasenaRepository extends JpaRepository<PoliticaContrasena, Integer> { }
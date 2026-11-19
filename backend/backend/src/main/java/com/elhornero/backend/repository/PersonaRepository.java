package com.elhornero.backend.repository;

import com.elhornero.backend.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByNumeroDocumentoIgnoreCase(String numeroDocumento);

}

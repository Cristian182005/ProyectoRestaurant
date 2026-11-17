package com.elhornero.backend.repository;

import com.elhornero.backend.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

	Optional<Usuario> findByPersonaEmailIgnoreCase(String email);

	boolean existsByPersonaEmailIgnoreCase(String email);

	Optional<Usuario> findByPersonaId(Integer personaId);

}

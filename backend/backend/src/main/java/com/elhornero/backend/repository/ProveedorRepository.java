package com.elhornero.backend.repository;

import com.elhornero.backend.entity.Proveedor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

	Optional<Proveedor> findByRucIgnoreCase(String ruc);

	boolean existsByRucIgnoreCase(String ruc);

	boolean existsByEmailIgnoreCase(String email);

}

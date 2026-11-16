package com.elhornero.backend.repository;

import com.elhornero.backend.entity.IngredientePlato;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientePlatoRepository extends JpaRepository<IngredientePlato, Integer> {

	List<IngredientePlato> findByPlatoId(Integer platoId);

	List<IngredientePlato> findByInsumoId(Integer insumoId);

	Optional<IngredientePlato> findByPlatoIdAndInsumoId(Integer platoId, Integer insumoId);

	boolean existsByPlatoId(Integer platoId);

	boolean existsByInsumoId(Integer insumoId);

	void deleteByPlatoId(Integer platoId);

}

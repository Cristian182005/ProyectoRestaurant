package com.elhornero.backend.repository;

import com.elhornero.backend.entity.Insumo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InsumoRepository extends JpaRepository<Insumo, Integer> {

	Optional<Insumo> findByNombreIgnoreCase(String nombre);

	boolean existsByNombreIgnoreCase(String nombre);

	List<Insumo> findByStockActualLessThanEqual(Double stockMinimo);

	@Query("select i from Insumo i where (:buscar is null or lower(i.nombre) like lower(concat('%', cast(:buscar as string), '%'))) "
			+ "order by i.nombre")
	List<Insumo> buscar(@Param("buscar") String buscar);

}

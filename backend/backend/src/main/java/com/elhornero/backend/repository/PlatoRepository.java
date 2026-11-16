package com.elhornero.backend.repository;

import com.elhornero.backend.entity.Plato;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatoRepository extends JpaRepository<Plato, Integer> {

	Optional<Plato> findByNombreIgnoreCase(String nombre);

	List<Plato> findByCategoriaIgnoreCase(String categoria);

	List<Plato> findByActivoTrue();

	boolean existsByCategoriaIgnoreCase(String categoria);

	boolean existsByNombreIgnoreCase(String nombre);

	@Query("select distinct p.categoria from Plato p where p.categoria is not null order by p.categoria")
	List<String> listarCategoriasDistinct();

	@Query("select p from Plato p where (:categoria is null or lower(p.categoria) = lower(cast(:categoria as string))) "
			+ "and (:activo is null or p.activo = :activo) "
			+ "and (:buscar is null or lower(p.nombre) like lower(concat('%', cast(:buscar as string), '%'))) "
			+ "order by p.nombre")
	List<Plato> buscar(@Param("categoria") String categoria, @Param("activo") Boolean activo,
			@Param("buscar") String buscar);

}

package com.elhornero.backend.repository;

import com.elhornero.backend.entity.IngresoInsumo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngresoInsumoRepository extends JpaRepository<IngresoInsumo, Integer> {

	@Query("select i from IngresoInsumo i left join fetch i.detalles where i.id = :id")
	IngresoInsumo findByIdConDetalles(@Param("id") Integer id);

	@Query("select i from IngresoInsumo i left join fetch i.detalles order by i.fechaIngreso desc")
	List<IngresoInsumo> listarConDetalles();

	boolean existsByProveedorId(Integer proveedorId);

	boolean existsByEmpleadoId(Integer empleadoId);

}

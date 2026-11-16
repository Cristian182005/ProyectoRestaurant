package com.elhornero.backend.repository;

import com.elhornero.backend.entity.DetalleIngreso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleIngresoRepository extends JpaRepository<DetalleIngreso, Integer> {

	List<DetalleIngreso> findByIngresoInsumoId(Integer ingresoInsumoId);

	boolean existsByInsumoId(Integer insumoId);

}

package com.elhornero.backend.repository;

import com.elhornero.backend.entity.Merma;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MermaRepository extends JpaRepository<Merma, Integer> {

	List<Merma> findByInsumoIdOrderByFechaRegistroDesc(Integer insumoId);

	List<Merma> findByEmpleadoIdOrderByFechaRegistroDesc(Integer empleadoId);

	List<Merma> findAllByOrderByFechaRegistroDesc();

}

package com.elhornero.backend.repository;

import com.elhornero.backend.entity.Mesa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MesaRepository extends JpaRepository<Mesa, Integer> {

	List<Mesa> findByEstadoIgnoreCase(String estado);

	boolean existsByNumero(Integer numero);

	boolean existsByNumeroAndIdNot(Integer numero, Integer id);

}

package com.elhornero.backend.repository;

import com.elhornero.backend.entity.DetallePedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Integer> {

	List<DetallePedido> findByPedidoId(Integer pedidoId);

	boolean existsByPedidoId(Integer pedidoId);

	long countByPedidoId(Integer pedidoId);

	boolean existsByPlatoId(Integer platoId);

	void deleteByPedidoId(Integer pedidoId);

}

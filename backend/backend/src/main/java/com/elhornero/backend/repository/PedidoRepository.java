package com.elhornero.backend.repository;

import com.elhornero.backend.entity.EstadoPedido;
import com.elhornero.backend.entity.Pedido;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

	@Query("select p from Pedido p left join fetch p.detalles where p.id = :id")
	Optional<Pedido> findByIdConDetalles(@Param("id") Integer id);

	@Query("select p from Pedido p left join fetch p.detalles order by p.horaTomadaDelPedido desc")
	List<Pedido> listarConDetalles();

	@Query("select p from Pedido p left join fetch p.detalles where p.estado = :estado "
			+ "order by p.horaTomadaDelPedido desc")
	List<Pedido> listarPorEstadoConDetalles(@Param("estado") EstadoPedido estado);

	@Query("select p from Pedido p left join fetch p.detalles "
			+ "where p.horaTomadaDelPedido between :desde and :hasta "
			+ "order by p.horaTomadaDelPedido desc")
	List<Pedido> listarEntreFechasConDetalles(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

	boolean existsByMesaIdAndEstadoIn(Integer mesaId, Collection<EstadoPedido> estados);

	boolean existsByMesaId(Integer mesaId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pedido p where p.id = :id")
	Optional<Pedido> findByIdParaBloqueo(@Param("id") Integer id);

}

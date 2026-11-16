package com.proyecto.restaurante.repository;

import com.proyecto.restaurante.entity.EstadoPedido;
import com.proyecto.restaurante.entity.Pedido;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);

    List<Pedido> findByEstadoOrderByFechaDesc(EstadoPedido estado);

    List<Pedido> findAllByOrderByFechaDesc();

    @Query("""
            SELECT p FROM Pedido p
            WHERE p.fecha >= :desde AND p.fecha < :hasta AND p.estado <> com.proyecto.restaurante.entity.EstadoPedido.CANCELADO
            """)
    List<Pedido> ventasActivas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}

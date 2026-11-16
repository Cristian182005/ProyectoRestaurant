package com.elhornero.backend.dto;

import com.elhornero.backend.entity.EstadoPedido;
import com.elhornero.backend.entity.Pedido;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(Integer id, EstadoPedido estado, LocalDateTime horaTomadaDelPedido,
		Integer mesaId, Integer mesaNumero, Integer empleadoId,
		List<DetallePedidoResponse> detalles) {

	public static PedidoResponse from(Pedido pedido) {
		List<DetallePedidoResponse> detalles = pedido.getDetalles() == null ? List.of()
				: pedido.getDetalles().stream().map(DetallePedidoResponse::from).toList();
		return new PedidoResponse(pedido.getId(), pedido.getEstado(), pedido.getHoraTomadaDelPedido(),
				pedido.getMesa() != null ? pedido.getMesa().getId() : null,
				pedido.getMesa() != null ? pedido.getMesa().getNumero() : null,
				pedido.getEmpleado() != null ? pedido.getEmpleado().getId() : null, detalles);
	}

}

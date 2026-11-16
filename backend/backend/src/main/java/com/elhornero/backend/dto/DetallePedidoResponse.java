package com.elhornero.backend.dto;

import com.elhornero.backend.entity.DetallePedido;

public record DetallePedidoResponse(Integer id, Integer pedidoId, Integer platoId, String platoNombre,
		Integer cantidad, Double precioUnitario, Double subtotal) {

	public static DetallePedidoResponse from(DetallePedido detalle) {
		return new DetallePedidoResponse(detalle.getId(),
				detalle.getPedido() != null ? detalle.getPedido().getId() : null,
				detalle.getPlato().getId(), detalle.getPlato().getNombre(), detalle.getCantidad(),
				detalle.getPrecioUnitario(), detalle.getSubtotal());
	}

}

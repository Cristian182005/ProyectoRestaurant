package com.elhornero.backend.dto;

import com.elhornero.backend.entity.DetalleIngreso;

public record DetalleIngresoResponse(Integer id, Integer ingresoId, Integer insumoId,
		String insumoNombre, Double cantidad) {

	public static DetalleIngresoResponse from(DetalleIngreso detalle) {
		return new DetalleIngresoResponse(detalle.getId(),
				detalle.getIngresoInsumo() != null ? detalle.getIngresoInsumo().getId() : null,
				detalle.getInsumo().getId(), detalle.getInsumo().getNombre(), detalle.getCantidad());
	}

}

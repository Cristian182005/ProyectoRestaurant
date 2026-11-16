package com.elhornero.backend.dto;

import com.elhornero.backend.entity.IngresoInsumo;
import java.time.LocalDate;
import java.util.List;

public record IngresoInsumoResponse(Integer id, LocalDate fechaIngreso, Integer proveedorId,
		Integer empleadoId, List<DetalleIngresoResponse> detalles) {

	public static IngresoInsumoResponse from(IngresoInsumo ingreso) {
		List<DetalleIngresoResponse> detalles = ingreso.getDetalles() == null ? List.of()
				: ingreso.getDetalles().stream().map(DetalleIngresoResponse::from).toList();
		return new IngresoInsumoResponse(ingreso.getId(), ingreso.getFechaIngreso(),
				ingreso.getProveedor() != null ? ingreso.getProveedor().getId() : null,
				ingreso.getEmpleado() != null ? ingreso.getEmpleado().getId() : null, detalles);
	}

}

package com.elhornero.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record IngresoRequest(Integer proveedorId, Integer empleadoId, LocalDate fechaIngreso,
		List<DetalleIngresoRequest> detalles) {
}

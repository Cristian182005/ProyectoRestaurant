package com.elhornero.backend.dto;

import java.time.LocalDate;

public record VentasResumenResponse(LocalDate desde, LocalDate hasta, long cantidadComprobantes,
		Double totalVentas, Double totalIgv, Double promedioTicket) {
}

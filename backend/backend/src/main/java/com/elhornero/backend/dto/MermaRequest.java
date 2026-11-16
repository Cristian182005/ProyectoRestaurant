package com.elhornero.backend.dto;

public record MermaRequest(Integer insumoId, Double cantidad, String motivo, String observacion,
		Integer empleadoId) {
}

package com.elhornero.backend.dto;

public record InsumoRequest(String nombre, String descripcion, Double stockActual, Double stockMinimo,
		String unidadMedida, String imagen) {
}

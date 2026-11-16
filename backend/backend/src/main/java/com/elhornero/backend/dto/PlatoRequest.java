package com.elhornero.backend.dto;

public record PlatoRequest(String nombre, String descripcion, String imagen, Boolean activo,
		String categoria, Double precio) {
}

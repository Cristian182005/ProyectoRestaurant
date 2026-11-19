package com.elhornero.backend.dto;

public record ClienteRequest(String nombre, String tipoDocumento, String numeroDocumento,
		String email, String telefono, String direccion) {
}

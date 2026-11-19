package com.elhornero.backend.dto;

import com.elhornero.backend.entity.Persona;

public record ClienteResponse(Integer id, String nombre, String tipoDocumento,
		String numeroDocumento, String email, String telefono, String direccion) {

	public static ClienteResponse from(Persona persona) {
		return new ClienteResponse(persona.getId(), persona.getNombre(), persona.getTipoDocumento(),
				persona.getNumeroDocumento(), persona.getEmail(), persona.getTelefono(),
				persona.getDireccion());
	}

}

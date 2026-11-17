package com.elhornero.backend.dto;

import com.elhornero.backend.entity.Persona;
import com.elhornero.backend.entity.Usuario;

public record UsuarioResponseDTO(Integer id, String email, String rol, String telefono,
		String direccion) {

	public static UsuarioResponseDTO from(Usuario usuario) {
		Persona persona = usuario.getPersona();
		return new UsuarioResponseDTO(usuario.getId(),
				persona != null ? persona.getEmail() : null,
				persona != null ? persona.getRol() : null,
				persona != null ? persona.getTelefono() : null,
				persona != null ? persona.getDireccion() : null);
	}

}

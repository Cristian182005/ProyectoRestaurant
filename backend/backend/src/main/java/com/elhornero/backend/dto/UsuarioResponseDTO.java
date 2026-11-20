package com.elhornero.backend.dto;

import com.elhornero.backend.entity.Persona;
import com.elhornero.backend.entity.Usuario;

/**
 * Vista publica de un usuario. Nunca expone la contrasena ni su hash.
 */
public record UsuarioResponseDTO(Integer id, String nombre, String email, String rol,
		String telefono, String direccion) {

	public static UsuarioResponseDTO from(Usuario usuario) {
		Persona persona = usuario.getPersona();
		return new UsuarioResponseDTO(usuario.getId(),
				persona != null ? persona.getNombre() : null,
				persona != null ? persona.getEmail() : null,
				persona != null ? persona.getRol() : null,
				persona != null ? persona.getTelefono() : null,
				persona != null ? persona.getDireccion() : null);
	}

}

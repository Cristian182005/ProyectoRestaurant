package com.elhornero.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Datos editables de un usuario desde el modulo de administracion: no
 * incluye el rol (gestion por separado) ni la contrasena.
 */
public record ActualizarUsuarioDTO(
		@Size(max = 100, message = "El nombre no puede superar 100 caracteres") String nombre,
		@Email(message = "El correo no tiene un formato valido")
		@Size(max = 150, message = "El correo no puede superar 150 caracteres") String email,
		@Size(max = 20, message = "El telefono no puede superar 20 caracteres") String telefono,
		@Size(max = 255, message = "La direccion no puede superar 255 caracteres") String direccion) {
}

package com.elhornero.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Nueva contrasena de un usuario. Se guarda siempre codificada con
 * BCrypt; el hash nunca sale en la respuesta.
 */
public record CambioPasswordDTO(
		@NotBlank(message = "La contrasena es obligatoria")
		@Size(min = 6, max = 255, message = "La contrasena debe tener entre 6 y 255 caracteres")
		String password) {
}

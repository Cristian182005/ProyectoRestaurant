package com.elhornero.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank(message = "El correo es obligatorio")
		@Email(message = "El correo no tiene un formato valido")
		@Size(max = 150, message = "El correo no puede superar 150 caracteres") String email,
		@NotBlank(message = "La contrasena es obligatoria")
		@Size(max = 255, message = "La contrasena no puede superar 255 caracteres")
		String password) {
}

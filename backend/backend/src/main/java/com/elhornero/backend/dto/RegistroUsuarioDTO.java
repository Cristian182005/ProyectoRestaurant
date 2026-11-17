package com.elhornero.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroUsuarioDTO(@NotBlank(message = "El correo es obligatorio")
		@Email(message = "El correo no tiene un formato valido")
		@Size(max = 150, message = "El correo no puede superar 150 caracteres") String email,
		@NotBlank(message = "La contrasena es obligatoria")
		@Size(min = 6, max = 255, message = "La contrasena debe tener entre 6 y 255 caracteres")
		String password,
		@Size(max = 20, message = "El telefono no puede superar 20 caracteres") String telefono,
		@Size(max = 255, message = "La direccion no puede superar 255 caracteres") String direccion,
		@Size(max = 50, message = "El rol no puede superar 50 caracteres") String rol) {
}

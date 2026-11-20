package com.elhornero.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cambio de rol de un usuario. Solo se admiten los roles operativos del
 * sistema: ADMINISTRADOR, MOZO y CAJERO.
 */
public record CambioRolDTO(@NotBlank(message = "El rol es obligatorio")
		@Size(max = 50, message = "El rol no puede superar 50 caracteres")
		@Pattern(regexp = "(?i)\\s*(MOZO|CAJERO|ADMINISTRADOR)\\s*", message = "Rol no valido: se admiten ADMINISTRADOR, CAJERO o MOZO")
		String rol) {
}

package com.elhornero.backend.security;

import com.elhornero.backend.exception.DatoInvalidoException;
import java.util.Arrays;

/**
 * Roles soportados por el sistema. Deben coincidir con los que usa el
 * frontend (usuarios-roles.ts): ADMINISTRADOR, CAJERO y MOZO.
 */
public enum RolUsuario {

	ADMINISTRADOR, CAJERO, MOZO;

	/**
	 * Convierte un rol en texto al enum, normalizando mayusculas/minusculas.
	 * Lanza DatoInvalidoException si el rol es nulo o no esta soportado.
	 */
	public static RolUsuario desde(String valor) {
		if (valor == null || valor.isBlank()) {
			throw new DatoInvalidoException("El rol es obligatorio");
		}
		String normalizado = valor.trim().toUpperCase();
		return Arrays.stream(values())
				.filter(rol -> rol.name().equals(normalizado))
				.findFirst()
				.orElseThrow(() -> new DatoInvalidoException(
						"Rol no valido: se admiten ADMINISTRADOR, CAJERO o MOZO"));
	}

}

package com.elhornero.backend.dto;

import java.time.LocalDateTime;

public record AuthResponseDTO(String token, String tipo, String email, String rol,
		LocalDateTime expiraEn, Integer id, Integer empleadoId) {

	public static AuthResponseDTO bearer(String token, String email, String rol,
			LocalDateTime expiraEn, Integer id, Integer empleadoId) {
		return new AuthResponseDTO(token, "Bearer", email, rol, expiraEn, id, empleadoId);
	}

}

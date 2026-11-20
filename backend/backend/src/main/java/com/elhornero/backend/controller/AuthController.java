package com.elhornero.backend.controller;

import com.elhornero.backend.dto.AuthResponseDTO;
import com.elhornero.backend.dto.LoginRequest;
import com.elhornero.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint publico de autenticacion: unico inicio de sesion que emite el
 * token JWT. El alta de usuarios vive en {@code /api/usuarios} y es
 * exclusiva del rol ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.autenticar(request));
	}

}

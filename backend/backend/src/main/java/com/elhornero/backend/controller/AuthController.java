package com.elhornero.backend.controller;

import com.elhornero.backend.dto.AuthResponseDTO;
import com.elhornero.backend.dto.LoginRequest;
import com.elhornero.backend.dto.RegistroUsuarioDTO;
import com.elhornero.backend.dto.UsuarioResponseDTO;
import com.elhornero.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publicos de autenticacion: registro de usuarios (solo roles
 * MOZO/CAJERO) e inicio de sesion que emite el token JWT.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/registro")
	public ResponseEntity<UsuarioResponseDTO> registrar(
			@Valid @RequestBody RegistroUsuarioDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarse(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.autenticar(request));
	}

}

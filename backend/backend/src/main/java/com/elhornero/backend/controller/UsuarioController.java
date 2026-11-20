package com.elhornero.backend.controller;

import com.elhornero.backend.dto.ActualizarUsuarioDTO;
import com.elhornero.backend.dto.CambioPasswordDTO;
import com.elhornero.backend.dto.CambioRolDTO;
import com.elhornero.backend.dto.RegistroUsuarioDTO;
import com.elhornero.backend.dto.UsuarioResponseDTO;
import com.elhornero.backend.service.UsuarioService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion de usuarios y roles, exclusiva del rol ADMINISTRADOR (protegida
 * en SecurityConfig). Reutiliza UsuarioService, los DTOs existentes y
 * BCrypt para las contrasenas. No hay registro publico.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping
	public ResponseEntity<List<UsuarioResponseDTO>> listar(
			@RequestParam(required = false) String buscar) {
		if (buscar != null && !buscar.isBlank()) {
			return ResponseEntity.ok(usuarioService.buscar(buscar.trim()));
		}
		return ResponseEntity.ok(usuarioService.listar());
	}

	@GetMapping("/{id:\\d+}")
	public ResponseEntity<UsuarioResponseDTO> obtener(@PathVariable Integer id) {
		return ResponseEntity.ok(usuarioService.buscarPorId(id));
	}

	@PostMapping
	public ResponseEntity<UsuarioResponseDTO> crear(
			@Valid @RequestBody RegistroUsuarioDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
	}

	@PutMapping("/{id:\\d+}")
	public ResponseEntity<UsuarioResponseDTO> actualizar(@PathVariable Integer id,
			@Valid @RequestBody ActualizarUsuarioDTO request) {
		return ResponseEntity.ok(usuarioService.actualizar(id, request));
	}

	@PutMapping("/{id:\\d+}/rol")
	public ResponseEntity<UsuarioResponseDTO> cambiarRol(@PathVariable Integer id,
			@Valid @RequestBody CambioRolDTO request, Principal principal) {
		return ResponseEntity.ok(usuarioService.cambiarRol(id, request.rol(),
				principal == null ? null : principal.getName()));
	}

	@PutMapping("/{id:\\d+}/password")
	public ResponseEntity<UsuarioResponseDTO> cambiarPassword(@PathVariable Integer id,
			@Valid @RequestBody CambioPasswordDTO request) {
		return ResponseEntity.ok(usuarioService.cambiarPassword(id, request));
	}

}

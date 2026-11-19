package com.elhornero.backend.controller;

import com.elhornero.backend.dto.ClienteRequest;
import com.elhornero.backend.dto.ClienteResponse;
import com.elhornero.backend.service.ClienteService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes/naturales")
public class ClienteNaturalController {

	private final ClienteService clienteService;

	public ClienteNaturalController(ClienteService clienteService) {
		this.clienteService = clienteService;
	}

	@GetMapping
	public ResponseEntity<List<ClienteResponse>> listar() {
		return ResponseEntity.ok(clienteService.listarNaturales());
	}

	@PostMapping
	public ResponseEntity<ClienteResponse> crear(@RequestBody ClienteRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crearNatural(request));
	}

}

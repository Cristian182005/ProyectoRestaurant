package com.elhornero.backend.controller;

import com.elhornero.backend.dto.ProveedorRequest;
import com.elhornero.backend.dto.ProveedorResponse;
import com.elhornero.backend.service.ProveedorService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

	private final ProveedorService proveedorService;

	public ProveedorController(ProveedorService proveedorService) {
		this.proveedorService = proveedorService;
	}

	@GetMapping
	public ResponseEntity<List<ProveedorResponse>> listar() {
		return ResponseEntity.ok(proveedorService.listar());
	}

	@PostMapping
	public ResponseEntity<ProveedorResponse> crear(@RequestBody ProveedorRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.crear(request));
	}

}

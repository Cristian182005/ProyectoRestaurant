package com.elhornero.backend.controller;

import com.elhornero.backend.dto.MesaRequest;
import com.elhornero.backend.entity.Mesa;
import com.elhornero.backend.service.MesaService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mesas")
public class MesaController {

	private final MesaService mesaService;

	public MesaController(MesaService mesaService) {
		this.mesaService = mesaService;
	}

	@GetMapping
	public ResponseEntity<List<Mesa>> listar(
			@RequestParam(required = false) String estado) {
		if (estado == null || estado.isBlank()) {
			return ResponseEntity.ok(mesaService.listar());
		}
		return ResponseEntity.ok(mesaService.listarPorEstado(estado));
	}

	@GetMapping("/{id:\\d+}")
	public ResponseEntity<Mesa> obtener(@PathVariable Integer id) {
		return ResponseEntity.ok(mesaService.obtener(id));
	}

	@PostMapping
	public ResponseEntity<Mesa> crear(@RequestBody MesaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(mesaService.crear(request));
	}

	@PutMapping("/{id:\\d+}")
	public ResponseEntity<Mesa> actualizar(@PathVariable Integer id,
			@RequestBody MesaRequest request) {
		return ResponseEntity.ok(mesaService.actualizar(id, request));
	}

	@DeleteMapping("/{id:\\d+}")
	public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
		mesaService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

}

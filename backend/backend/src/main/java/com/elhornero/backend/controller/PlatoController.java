package com.elhornero.backend.controller;

import com.elhornero.backend.dto.PlatoRequest;
import com.elhornero.backend.entity.Plato;
import com.elhornero.backend.service.PlatoService;
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
@RequestMapping("/api/platos")
public class PlatoController {

	private final PlatoService platoService;

	public PlatoController(PlatoService platoService) {
		this.platoService = platoService;
	}

	@GetMapping
	public ResponseEntity<List<Plato>> listar(@RequestParam(required = false) String categoria,
			@RequestParam(required = false) Boolean activo,
			@RequestParam(required = false) String buscar) {
		return ResponseEntity.ok(platoService.listar(categoria, activo, buscar));
	}

	@GetMapping("/categorias")
	public ResponseEntity<List<String>> listarCategorias() {
		return ResponseEntity.ok(platoService.listarCategorias());
	}

	@GetMapping("/{id:\\d+}")
	public ResponseEntity<Plato> obtener(@PathVariable Integer id) {
		return ResponseEntity.ok(platoService.obtener(id));
	}

	@PostMapping
	public ResponseEntity<Plato> crear(@RequestBody PlatoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(platoService.crear(request));
	}

	@PutMapping("/{id:\\d+}")
	public ResponseEntity<Plato> actualizar(@PathVariable Integer id,
			@RequestBody PlatoRequest request) {
		return ResponseEntity.ok(platoService.actualizar(id, request));
	}

	@DeleteMapping("/{id:\\d+}")
	public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
		platoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

}

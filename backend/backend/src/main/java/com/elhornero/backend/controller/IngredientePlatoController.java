package com.elhornero.backend.controller;

import com.elhornero.backend.dto.CantidadRequeridaRequest;
import com.elhornero.backend.dto.IngredientePlatoRequest;
import com.elhornero.backend.entity.IngredientePlato;
import com.elhornero.backend.service.IngredientePlatoService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingredientes")
public class IngredientePlatoController {

	private final IngredientePlatoService ingredientePlatoService;

	public IngredientePlatoController(IngredientePlatoService ingredientePlatoService) {
		this.ingredientePlatoService = ingredientePlatoService;
	}

	@GetMapping("/plato/{platoId:\\d+}")
	public ResponseEntity<List<IngredientePlato>> listarPorPlato(
			@PathVariable Integer platoId) {
		return ResponseEntity.ok(ingredientePlatoService.listarPorPlato(platoId));
	}

	@GetMapping("/insumo/{insumoId:\\d+}")
	public ResponseEntity<List<IngredientePlato>> listarPorInsumo(
			@PathVariable Integer insumoId) {
		return ResponseEntity.ok(ingredientePlatoService.listarPorInsumo(insumoId));
	}

	@GetMapping("/{id:\\d+}")
	public ResponseEntity<IngredientePlato> obtener(@PathVariable Integer id) {
		return ResponseEntity.ok(ingredientePlatoService.obtener(id));
	}

	@PostMapping
	public ResponseEntity<IngredientePlato> crear(
			@RequestBody IngredientePlatoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ingredientePlatoService.crear(request));
	}

	@PutMapping("/{id:\\d+}")
	public ResponseEntity<IngredientePlato> actualizar(@PathVariable Integer id,
			@RequestBody CantidadRequeridaRequest request) {
		return ResponseEntity.ok(ingredientePlatoService.actualizar(id,
				request == null ? null : request.cantidadRequerida()));
	}

	@DeleteMapping("/{id:\\d+}")
	public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
		ingredientePlatoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

}

package com.elhornero.backend.controller;

import com.elhornero.backend.dto.InsumoRequest;
import com.elhornero.backend.entity.Insumo;
import com.elhornero.backend.service.InsumoService;
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
@RequestMapping("/api/insumos")
public class InsumoController {

	private final InsumoService insumoService;

	public InsumoController(InsumoService insumoService) {
		this.insumoService = insumoService;
	}

	@GetMapping
	public ResponseEntity<List<Insumo>> listar(
			@RequestParam(required = false) String buscar) {
		return ResponseEntity.ok(insumoService.listar(buscar));
	}

	@GetMapping("/stock-bajo")
	public ResponseEntity<List<Insumo>> listarStockBajo() {
		return ResponseEntity.ok(insumoService.listarStockBajo());
	}

	@GetMapping("/{id:\\d+}")
	public ResponseEntity<Insumo> obtener(@PathVariable Integer id) {
		return ResponseEntity.ok(insumoService.obtener(id));
	}

	@PostMapping
	public ResponseEntity<Insumo> crear(@RequestBody InsumoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(insumoService.crear(request));
	}

	@PutMapping("/{id:\\d+}")
	public ResponseEntity<Insumo> actualizar(@PathVariable Integer id,
			@RequestBody InsumoRequest request) {
		return ResponseEntity.ok(insumoService.actualizar(id, request));
	}

	@DeleteMapping("/{id:\\d+}")
	public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
		insumoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id:\\d+}/descontar-stock")
	public ResponseEntity<Insumo> descontarStock(@PathVariable Integer id,
			@RequestParam Double cantidad) {
		return ResponseEntity.ok(insumoService.descontarStock(id, cantidad));
	}

	@PostMapping("/{id:\\d+}/aumentar-stock")
	public ResponseEntity<Insumo> aumentarStock(@PathVariable Integer id,
			@RequestParam Double cantidad) {
		return ResponseEntity.ok(insumoService.aumentarStock(id, cantidad));
	}

}

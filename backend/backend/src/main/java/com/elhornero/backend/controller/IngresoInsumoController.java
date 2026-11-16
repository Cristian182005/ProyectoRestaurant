package com.elhornero.backend.controller;

import com.elhornero.backend.dto.DetalleIngresoResponse;
import com.elhornero.backend.dto.IngresoInsumoResponse;
import com.elhornero.backend.dto.IngresoRequest;
import com.elhornero.backend.service.IngresoInsumoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingresos")
public class IngresoInsumoController {

	private final IngresoInsumoService ingresoInsumoService;

	public IngresoInsumoController(IngresoInsumoService ingresoInsumoService) {
		this.ingresoInsumoService = ingresoInsumoService;
	}

	@GetMapping
	public ResponseEntity<List<IngresoInsumoResponse>> listar() {
		return ResponseEntity.ok(ingresoInsumoService.listar().stream()
				.map(IngresoInsumoResponse::from).toList());
	}

	@GetMapping("/{id:\\d+}")
	public ResponseEntity<IngresoInsumoResponse> obtener(@PathVariable Integer id) {
		return ResponseEntity.ok(IngresoInsumoResponse.from(ingresoInsumoService.obtener(id)));
	}

	@GetMapping("/{id:\\d+}/detalles")
	public ResponseEntity<List<DetalleIngresoResponse>> listarDetalles(
			@PathVariable Integer id) {
		return ResponseEntity.ok(ingresoInsumoService.listarDetalles(id).stream()
				.map(DetalleIngresoResponse::from).toList());
	}

	@PostMapping
	public ResponseEntity<IngresoInsumoResponse> crear(@RequestBody IngresoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(IngresoInsumoResponse.from(ingresoInsumoService.crear(request)));
	}

}

package com.elhornero.backend.controller;

import com.elhornero.backend.dto.ComprobanteRequest;
import com.elhornero.backend.dto.ComprobanteResponse;
import com.elhornero.backend.dto.VentasResumenResponse;
import com.elhornero.backend.service.ComprobanteService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comprobantes")
public class ComprobanteController {

	private final ComprobanteService comprobanteService;

	public ComprobanteController(ComprobanteService comprobanteService) {
		this.comprobanteService = comprobanteService;
	}

	@GetMapping
	public ResponseEntity<List<ComprobanteResponse>> listar(
			@RequestParam(required = false) LocalDate desde,
			@RequestParam(required = false) LocalDate hasta) {
		if (desde == null && hasta == null) {
			return ResponseEntity.ok(comprobanteService.listar().stream()
					.map(ComprobanteResponse::from).toList());
		}
		return ResponseEntity.ok(comprobanteService.listarEntreFechas(desde, hasta).stream()
				.map(ComprobanteResponse::from).toList());
	}

	@GetMapping("/resumen")
	public ResponseEntity<VentasResumenResponse> resumen(@RequestParam LocalDate desde,
			@RequestParam LocalDate hasta) {
		return ResponseEntity.ok(comprobanteService.resumen(desde, hasta));
	}

	@GetMapping("/pedido/{pedidoId:\\d+}")
	public ResponseEntity<ComprobanteResponse> obtenerPorPedido(
			@PathVariable Integer pedidoId) {
		return ResponseEntity.ok(
				ComprobanteResponse.from(comprobanteService.obtenerPorPedido(pedidoId)));
	}

	@GetMapping("/{id:\\d+}")
	public ResponseEntity<ComprobanteResponse> obtener(@PathVariable Integer id) {
		return ResponseEntity.ok(ComprobanteResponse.from(comprobanteService.obtener(id)));
	}

	@PostMapping
	public ResponseEntity<ComprobanteResponse> emitir(@RequestBody ComprobanteRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ComprobanteResponse.from(comprobanteService.emitir(request)));
	}

}

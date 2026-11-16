package com.elhornero.backend.controller;

import com.elhornero.backend.dto.DetallePedidoResponse;
import com.elhornero.backend.dto.EstadoPedidoRequest;
import com.elhornero.backend.dto.ItemPedidoRequest;
import com.elhornero.backend.dto.PedidoRequest;
import com.elhornero.backend.dto.PedidoResponse;
import com.elhornero.backend.entity.EstadoPedido;
import com.elhornero.backend.service.DetallePedidoService;
import com.elhornero.backend.service.PedidoService;
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
@RequestMapping("/api/pedidos")
public class PedidoController {

	private final PedidoService pedidoService;
	private final DetallePedidoService detallePedidoService;

	public PedidoController(PedidoService pedidoService,
			DetallePedidoService detallePedidoService) {
		this.pedidoService = pedidoService;
		this.detallePedidoService = detallePedidoService;
	}

	@GetMapping
	public ResponseEntity<List<PedidoResponse>> listar(
			@RequestParam(required = false) EstadoPedido estado) {
		List<PedidoResponse> pedidos = (estado == null ? pedidoService.listar()
				: pedidoService.listarPorEstado(estado)).stream().map(PedidoResponse::from)
				.toList();
		return ResponseEntity.ok(pedidos);
	}

	@GetMapping("/{id:\\d+}")
	public ResponseEntity<PedidoResponse> obtener(@PathVariable Integer id) {
		return ResponseEntity.ok(PedidoResponse.from(pedidoService.obtener(id)));
	}

	@GetMapping("/{id:\\d+}/detalles")
	public ResponseEntity<List<DetallePedidoResponse>> listarDetalles(
			@PathVariable Integer id) {
		return ResponseEntity.ok(detallePedidoService.listarPorPedido(id).stream()
				.map(DetallePedidoResponse::from).toList());
	}

	@PostMapping
	public ResponseEntity<PedidoResponse> crear(@RequestBody PedidoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(PedidoResponse.from(pedidoService.crear(request)));
	}

	@PostMapping("/{id:\\d+}/detalles")
	public ResponseEntity<DetallePedidoResponse> agregarDetalle(@PathVariable Integer id,
			@RequestBody ItemPedidoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(DetallePedidoResponse.from(detallePedidoService.agregarDetalle(id, request)));
	}

	@PutMapping("/{id:\\d+}/estado")
	public ResponseEntity<PedidoResponse> cambiarEstado(@PathVariable Integer id,
			@RequestBody EstadoPedidoRequest request) {
		return ResponseEntity.ok(PedidoResponse.from(pedidoService.cambiarEstado(id, request)));
	}

	@DeleteMapping("/{id:\\d+}")
	public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
		pedidoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

}

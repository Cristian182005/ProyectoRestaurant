package com.elhornero.backend.controller;

import com.elhornero.backend.dto.CantidadRequest;
import com.elhornero.backend.dto.DetallePedidoResponse;
import com.elhornero.backend.service.DetallePedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/detalles-pedido")
public class DetallePedidoController {

	private final DetallePedidoService detallePedidoService;

	public DetallePedidoController(DetallePedidoService detallePedidoService) {
		this.detallePedidoService = detallePedidoService;
	}

	@GetMapping("/{id:\\d+}")
	public ResponseEntity<DetallePedidoResponse> obtener(@PathVariable Integer id) {
		return ResponseEntity.ok(DetallePedidoResponse.from(detallePedidoService.obtener(id)));
	}

	@PutMapping("/{id:\\d+}")
	public ResponseEntity<DetallePedidoResponse> actualizarCantidad(@PathVariable Integer id,
			@RequestBody CantidadRequest request) {
		return ResponseEntity.ok(DetallePedidoResponse
				.from(detallePedidoService.actualizarCantidad(id,
						request == null ? null : request.cantidad())));
	}

	@DeleteMapping("/{id:\\d+}")
	public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
		detallePedidoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

}

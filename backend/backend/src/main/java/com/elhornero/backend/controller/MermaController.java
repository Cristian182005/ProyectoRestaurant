package com.elhornero.backend.controller;

import com.elhornero.backend.dto.MermaRequest;
import com.elhornero.backend.entity.Merma;
import com.elhornero.backend.service.MermaService;
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
@RequestMapping("/api/mermas")
public class MermaController {

	private final MermaService mermaService;

	public MermaController(MermaService mermaService) {
		this.mermaService = mermaService;
	}

	@GetMapping
	public ResponseEntity<List<Merma>> listar() {
		return ResponseEntity.ok(mermaService.listar());
	}

	@GetMapping("/insumo/{insumoId:\\d+}")
	public ResponseEntity<List<Merma>> listarPorInsumo(@PathVariable Integer insumoId) {
		return ResponseEntity.ok(mermaService.listarPorInsumo(insumoId));
	}

	@GetMapping("/empleado/{empleadoId:\\d+}")
	public ResponseEntity<List<Merma>> listarPorEmpleado(@PathVariable Integer empleadoId) {
		return ResponseEntity.ok(mermaService.listarPorEmpleado(empleadoId));
	}

	@GetMapping("/{id:\\d+}")
	public ResponseEntity<Merma> obtener(@PathVariable Integer id) {
		return ResponseEntity.ok(mermaService.obtener(id));
	}

	@PostMapping
	public ResponseEntity<Merma> registrar(@RequestBody MermaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(mermaService.registrar(request));
	}

}

package com.elhornero.backend.controller;

import com.elhornero.backend.service.PlatoService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

	private final PlatoService platoService;

	public CategoriaController(PlatoService platoService) {
		this.platoService = platoService;
	}

	@GetMapping
	public ResponseEntity<List<String>> listar() {
		return ResponseEntity.ok(platoService.listarCategorias());
	}

}

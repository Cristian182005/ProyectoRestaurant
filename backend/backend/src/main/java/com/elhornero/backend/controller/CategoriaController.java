package com.elhornero.backend.controller;

import com.elhornero.backend.dto.CategoriaRequest;
import com.elhornero.backend.dto.CategoriaResponse;
import com.elhornero.backend.entity.Plato;
import com.elhornero.backend.service.CategoriaService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las categorias se derivan de los platos registrados, por lo que no existe
 * POST: una categoria nueva nace al registrar un plato con ese nombre.
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

	private final CategoriaService categoriaService;

	public CategoriaController(CategoriaService categoriaService) {
		this.categoriaService = categoriaService;
	}

	@GetMapping
	public ResponseEntity<List<String>> listar() {
		return ResponseEntity.ok(categoriaService.listar());
	}

	@GetMapping("/{nombre}")
	public ResponseEntity<CategoriaResponse> obtener(@PathVariable String nombre) {
		return ResponseEntity.ok(categoriaService.obtener(nombre));
	}

	@GetMapping("/{nombre}/productos")
	public ResponseEntity<List<Plato>> listarProductos(@PathVariable String nombre) {
		return ResponseEntity.ok(categoriaService.listarProductos(nombre));
	}

	@PutMapping("/{nombre}")
	public ResponseEntity<CategoriaResponse> renombrar(@PathVariable String nombre,
			@RequestBody CategoriaRequest request) {
		return ResponseEntity.ok(categoriaService.renombrar(nombre, request));
	}

	@DeleteMapping("/{nombre}")
	public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
		categoriaService.eliminar(nombre);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

}

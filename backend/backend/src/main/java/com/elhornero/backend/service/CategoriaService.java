package com.elhornero.backend.service;

import com.elhornero.backend.dto.CategoriaRequest;
import com.elhornero.backend.dto.CategoriaResponse;
import com.elhornero.backend.entity.Plato;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.exception.RecursoNoEncontradoException;
import com.elhornero.backend.repository.PlatoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion de categorias. Las categorias no tienen tabla propia: existen como
 * texto en la columna plato.categoria y se derivan de los platos registrados.
 */
@Service
public class CategoriaService {

	private static final int LONGITUD_MAXIMA = 50;

	private final PlatoRepository platoRepository;

	public CategoriaService(PlatoRepository platoRepository) {
		this.platoRepository = platoRepository;
	}

	@Transactional(readOnly = true)
	public List<String> listar() {
		return platoRepository.listarCategoriasDistinct();
	}

	@Transactional(readOnly = true)
	public CategoriaResponse obtener(String nombre) {
		String categoria = validarNombreExistente(nombre);
		return new CategoriaResponse(categoria, platoRepository.countByCategoriaIgnoreCase(categoria));
	}

	@Transactional(readOnly = true)
	public List<Plato> listarProductos(String nombre) {
		String categoria = validarNombreExistente(nombre);
		return platoRepository.findByCategoriaIgnoreCase(categoria);
	}

	@Transactional
	public CategoriaResponse renombrar(String nombreActual, CategoriaRequest request) {
		String actual = validarNombreExistente(nombreActual);
		String nuevo = validarNombreNuevo(request);
		if (actual.equalsIgnoreCase(nuevo)) {
			throw new DatoInvalidoException("El nuevo nombre de la categoria debe ser diferente al actual");
		}
		if (platoRepository.existsByCategoriaIgnoreCase(nuevo)) {
			throw new OperacionNoPermitidaException("Ya existe la categoria '" + nuevo + "'");
		}
		platoRepository.actualizarCategoria(actual, nuevo);
		return new CategoriaResponse(nuevo, platoRepository.countByCategoriaIgnoreCase(nuevo));
	}

	@Transactional
	public void eliminar(String nombre) {
		String categoria = validarNombreExistente(nombre);
		long productos = platoRepository.countByCategoriaIgnoreCase(categoria);
		if (productos > 0) {
			throw new OperacionNoPermitidaException(
					"No se puede eliminar la categoria porque tiene " + productos + " producto(s) relacionado(s)");
		}
	}

	private String validarNombreExistente(String nombre) {
		String categoria = validarNombre(nombre, "El nombre de la categoria es obligatorio");
		if (!platoRepository.existsByCategoriaIgnoreCase(categoria)) {
			throw new RecursoNoEncontradoException("Categoria no encontrada con nombre " + categoria);
		}
		return categoria;
	}

	private String validarNombreNuevo(CategoriaRequest request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos de la categoria son obligatorios");
		}
		return validarNombre(request.nombre(), "El nombre de la categoria es obligatorio");
	}

	private String validarNombre(String nombre, String mensajeObligatorio) {
		if (nombre == null || nombre.isBlank()) {
			throw new DatoInvalidoException(mensajeObligatorio);
		}
		String categoria = nombre.trim();
		if (categoria.length() > LONGITUD_MAXIMA) {
			throw new DatoInvalidoException(
					"El nombre de la categoria no puede superar " + LONGITUD_MAXIMA + " caracteres");
		}
		return categoria;
	}

}

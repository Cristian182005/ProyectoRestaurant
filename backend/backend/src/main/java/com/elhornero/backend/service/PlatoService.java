package com.elhornero.backend.service;

import com.elhornero.backend.dto.PlatoRequest;
import com.elhornero.backend.entity.Plato;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.exception.RecursoNoEncontradoException;
import com.elhornero.backend.repository.DetallePedidoRepository;
import com.elhornero.backend.repository.IngredientePlatoRepository;
import com.elhornero.backend.repository.PlatoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatoService {

	private static final String MSG_NO_ENCONTRADO = "Plato no encontrado con id ";

	private final PlatoRepository platoRepository;
	private final DetallePedidoRepository detallePedidoRepository;
	private final IngredientePlatoRepository ingredientePlatoRepository;

	public PlatoService(PlatoRepository platoRepository, DetallePedidoRepository detallePedidoRepository,
			IngredientePlatoRepository ingredientePlatoRepository) {
		this.platoRepository = platoRepository;
		this.detallePedidoRepository = detallePedidoRepository;
		this.ingredientePlatoRepository = ingredientePlatoRepository;
	}

	@Transactional(readOnly = true)
	public List<Plato> listar(String categoria, Boolean activo, String buscar) {
		String termino = (buscar == null || buscar.isBlank()) ? null : buscar.trim();
		String cat = (categoria == null || categoria.isBlank()) ? null : categoria.trim();
		return platoRepository.buscar(cat, activo, termino);
	}

	@Transactional(readOnly = true)
	public List<String> listarCategorias() {
		return platoRepository.listarCategoriasDistinct();
	}

	@Transactional(readOnly = true)
	public Plato obtener(Integer id) {
		return buscarPlato(id);
	}

	@Transactional
	public Plato crear(PlatoRequest request) {
		validarRequest(request);
		String nombre = request.nombre().trim();
		if (platoRepository.existsByNombreIgnoreCase(nombre)) {
			throw new OperacionNoPermitidaException("Ya existe un plato con el nombre '" + nombre + "'");
		}
		Plato plato = new Plato();
		aplicarDatos(plato, request);
		return platoRepository.save(plato);
	}

	@Transactional
	public Plato actualizar(Integer id, PlatoRequest request) {
		validarRequest(request);
		Plato plato = buscarPlato(id);
		String nombre = request.nombre().trim();
		platoRepository.findByNombreIgnoreCase(nombre)
				.filter(existente -> !existente.getId().equals(id))
				.ifPresent(existente -> {
					throw new OperacionNoPermitidaException(
							"Ya existe otro plato con el nombre '" + nombre + "'");
				});
		aplicarDatos(plato, request);
		return platoRepository.save(plato);
	}

	@Transactional
	public void eliminar(Integer id) {
		Plato plato = buscarPlato(id);
		if (plato.getActivo() != null && plato.getActivo()) {
			plato.setActivo(false);
			platoRepository.save(plato);
			return;
		}
		if (detallePedidoRepository.existsByPlatoId(id)) {
			throw new OperacionNoPermitidaException(
					"No se puede eliminar el plato porque tiene pedidos asociados");
		}
		ingredientePlatoRepository.deleteByPlatoId(id);
		platoRepository.delete(plato);
	}

	private void validarRequest(PlatoRequest request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos del plato son obligatorios");
		}
		if (request.nombre() == null || request.nombre().isBlank()) {
			throw new DatoInvalidoException("El nombre del plato es obligatorio");
		}
		if (request.categoria() == null || request.categoria().isBlank()) {
			throw new DatoInvalidoException("La categoría del plato es obligatoria");
		}
		if (request.precio() == null) {
			throw new DatoInvalidoException("El precio del plato es obligatorio");
		}
		if (request.precio() <= 0) {
			throw new DatoInvalidoException("El precio del plato debe ser mayor a 0");
		}
	}

	private void aplicarDatos(Plato plato, PlatoRequest request) {
		plato.setNombre(request.nombre().trim());
		plato.setDescripcion(request.descripcion());
		plato.setImagen(request.imagen());
		plato.setCategoria(request.categoria().trim());
		plato.setPrecio(request.precio());
		plato.setActivo(request.activo() == null || request.activo());
	}

	private Plato buscarPlato(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id del plato es obligatorio");
		}
		return platoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(MSG_NO_ENCONTRADO + id));
	}

}

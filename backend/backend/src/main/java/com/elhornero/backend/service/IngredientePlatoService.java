package com.elhornero.backend.service;

import com.elhornero.backend.dto.IngredientePlatoRequest;
import com.elhornero.backend.entity.IngredientePlato;
import com.elhornero.backend.entity.Insumo;
import com.elhornero.backend.entity.Plato;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.exception.RecursoNoEncontradoException;
import com.elhornero.backend.repository.IngredientePlatoRepository;
import com.elhornero.backend.repository.InsumoRepository;
import com.elhornero.backend.repository.PlatoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientePlatoService {

	private final IngredientePlatoRepository ingredientePlatoRepository;
	private final PlatoRepository platoRepository;
	private final InsumoRepository insumoRepository;

	public IngredientePlatoService(IngredientePlatoRepository ingredientePlatoRepository,
			PlatoRepository platoRepository, InsumoRepository insumoRepository) {
		this.ingredientePlatoRepository = ingredientePlatoRepository;
		this.platoRepository = platoRepository;
		this.insumoRepository = insumoRepository;
	}

	@Transactional(readOnly = true)
	public List<IngredientePlato> listarPorPlato(Integer platoId) {
		if (platoId == null) {
			throw new DatoInvalidoException("El id del plato es obligatorio");
		}
		if (!platoRepository.existsById(platoId)) {
			throw new RecursoNoEncontradoException("Plato no encontrado con id " + platoId);
		}
		return ingredientePlatoRepository.findByPlatoId(platoId);
	}

	@Transactional(readOnly = true)
	public List<IngredientePlato> listarPorInsumo(Integer insumoId) {
		if (insumoId == null) {
			throw new DatoInvalidoException("El id del insumo es obligatorio");
		}
		if (!insumoRepository.existsById(insumoId)) {
			throw new RecursoNoEncontradoException("Insumo no encontrado con id " + insumoId);
		}
		return ingredientePlatoRepository.findByInsumoId(insumoId);
	}

	@Transactional(readOnly = true)
	public IngredientePlato obtener(Integer id) {
		return buscarIngrediente(id);
	}

	@Transactional
	public IngredientePlato crear(IngredientePlatoRequest request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos del ingrediente son obligatorios");
		}
		validarCantidad(request.cantidadRequerida());
		Plato plato = buscarPlato(request.platoId());
		Insumo insumo = buscarInsumo(request.insumoId());
		if (ingredientePlatoRepository.findByPlatoIdAndInsumoId(plato.getId(), insumo.getId()).isPresent()) {
			throw new OperacionNoPermitidaException(
					"El plato '" + plato.getNombre() + "' ya tiene registrado el insumo '"
							+ insumo.getNombre() + "'");
		}
		IngredientePlato ingrediente = new IngredientePlato();
		ingrediente.setPlato(plato);
		ingrediente.setInsumo(insumo);
		ingrediente.setCantidadRequerida(request.cantidadRequerida());
		return ingredientePlatoRepository.save(ingrediente);
	}

	@Transactional
	public IngredientePlato actualizar(Integer id, Double cantidadRequerida) {
		validarCantidad(cantidadRequerida);
		IngredientePlato ingrediente = buscarIngrediente(id);
		ingrediente.setCantidadRequerida(cantidadRequerida);
		return ingredientePlatoRepository.save(ingrediente);
	}

	@Transactional
	public void eliminar(Integer id) {
		IngredientePlato ingrediente = buscarIngrediente(id);
		ingredientePlatoRepository.delete(ingrediente);
	}

	private void validarCantidad(Double cantidad) {
		if (cantidad == null) {
			throw new DatoInvalidoException("La cantidad requerida del insumo es obligatoria");
		}
		if (cantidad <= 0) {
			throw new DatoInvalidoException("La cantidad requerida del insumo debe ser mayor a 0");
		}
	}

	private Plato buscarPlato(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id del plato es obligatorio");
		}
		return platoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Plato no encontrado con id " + id));
	}

	private Insumo buscarInsumo(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id del insumo es obligatorio");
		}
		return insumoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado con id " + id));
	}

	private IngredientePlato buscarIngrediente(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id del ingrediente es obligatorio");
		}
		return ingredientePlatoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Ingrediente no encontrado con id " + id));
	}

}

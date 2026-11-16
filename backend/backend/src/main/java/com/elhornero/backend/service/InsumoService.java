package com.elhornero.backend.service;

import com.elhornero.backend.dto.InsumoRequest;
import com.elhornero.backend.entity.Insumo;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.exception.RecursoNoEncontradoException;
import com.elhornero.backend.repository.DetalleIngresoRepository;
import com.elhornero.backend.repository.IngredientePlatoRepository;
import com.elhornero.backend.repository.InsumoRepository;
import com.elhornero.backend.repository.MermaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsumoService {

	private static final String MSG_NO_ENCONTRADO = "Insumo no encontrado con id ";

	private final InsumoRepository insumoRepository;
	private final IngredientePlatoRepository ingredientePlatoRepository;
	private final DetalleIngresoRepository detalleIngresoRepository;
	private final MermaRepository mermaRepository;

	public InsumoService(InsumoRepository insumoRepository,
			IngredientePlatoRepository ingredientePlatoRepository,
			DetalleIngresoRepository detalleIngresoRepository, MermaRepository mermaRepository) {
		this.insumoRepository = insumoRepository;
		this.ingredientePlatoRepository = ingredientePlatoRepository;
		this.detalleIngresoRepository = detalleIngresoRepository;
		this.mermaRepository = mermaRepository;
	}

	@Transactional(readOnly = true)
	public List<Insumo> listar(String buscar) {
		String termino = (buscar == null || buscar.isBlank()) ? null : buscar.trim();
		return insumoRepository.buscar(termino);
	}

	@Transactional(readOnly = true)
	public List<Insumo> listarStockBajo() {
		return insumoRepository.findAll().stream()
				.filter(i -> i.getStockActual() != null && i.getStockMinimo() != null
						&& i.getStockActual() <= i.getStockMinimo())
				.toList();
	}

	@Transactional(readOnly = true)
	public Insumo obtener(Integer id) {
		return buscarInsumo(id);
	}

	@Transactional
	public Insumo crear(InsumoRequest request) {
		validarRequest(request);
		String nombre = request.nombre().trim();
		if (insumoRepository.existsByNombreIgnoreCase(nombre)) {
			throw new OperacionNoPermitidaException("Ya existe un insumo con el nombre '" + nombre + "'");
		}
		Insumo insumo = new Insumo();
		aplicarDatos(insumo, request);
		double stockActual = request.stockActual() == null ? 0.0 : request.stockActual();
		if (stockActual < 0) {
			throw new DatoInvalidoException("El stock actual no puede ser negativo");
		}
		insumo.setStockActual(stockActual);
		return insumoRepository.save(insumo);
	}

	@Transactional
	public Insumo actualizar(Integer id, InsumoRequest request) {
		validarRequest(request);
		Insumo insumo = buscarInsumo(id);
		String nombre = request.nombre().trim();
		insumoRepository.findByNombreIgnoreCase(nombre)
				.filter(existente -> !existente.getId().equals(id))
				.ifPresent(existente -> {
					throw new OperacionNoPermitidaException(
							"Ya existe otro insumo con el nombre '" + nombre + "'");
				});
		if (request.stockActual() != null && request.stockActual() < 0) {
			throw new DatoInvalidoException("El stock actual no puede ser negativo");
		}
		aplicarDatos(insumo, request);
		if (request.stockActual() != null) {
			insumo.setStockActual(request.stockActual());
		}
		return insumoRepository.save(insumo);
	}

	@Transactional
	public void eliminar(Integer id) {
		Insumo insumo = buscarInsumo(id);
		if (ingredientePlatoRepository.existsByInsumoId(id)) {
			throw new OperacionNoPermitidaException(
					"No se puede eliminar el insumo porque es ingrediente de platos");
		}
		if (detalleIngresoRepository.existsByInsumoId(id)) {
			throw new OperacionNoPermitidaException(
					"No se puede eliminar el insumo porque tiene ingresos registrados");
		}
		if (!mermaRepository.findByInsumoIdOrderByFechaRegistroDesc(id).isEmpty()) {
			throw new OperacionNoPermitidaException(
					"No se puede eliminar el insumo porque tiene mermas registradas");
		}
		insumoRepository.delete(insumo);
	}

	@Transactional
	public Insumo descontarStock(Integer id, Double cantidad) {
		if (cantidad == null || cantidad <= 0) {
			throw new DatoInvalidoException("La cantidad a descontar debe ser mayor a 0");
		}
		Insumo insumo = buscarInsumo(id);
		double actual = insumo.getStockActual() == null ? 0.0 : insumo.getStockActual();
		if (actual < cantidad) {
			throw new com.elhornero.backend.exception.StockInsuficienteException(
					"Stock insuficiente de '" + insumo.getNombre() + "': disponible " + actual
							+ ", requerido " + cantidad);
		}
		insumo.setStockActual(actual - cantidad);
		return insumoRepository.save(insumo);
	}

	@Transactional
	public Insumo aumentarStock(Integer id, Double cantidad) {
		if (cantidad == null || cantidad <= 0) {
			throw new DatoInvalidoException("La cantidad a ingresar debe ser mayor a 0");
		}
		Insumo insumo = buscarInsumo(id);
		double actual = insumo.getStockActual() == null ? 0.0 : insumo.getStockActual();
		insumo.setStockActual(actual + cantidad);
		return insumoRepository.save(insumo);
	}

	private void validarRequest(InsumoRequest request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos del insumo son obligatorios");
		}
		if (request.nombre() == null || request.nombre().isBlank()) {
			throw new DatoInvalidoException("El nombre del insumo es obligatorio");
		}
		if (request.unidadMedida() == null || request.unidadMedida().isBlank()) {
			throw new DatoInvalidoException("La unidad de medida del insumo es obligatoria");
		}
		if (request.stockMinimo() != null && request.stockMinimo() < 0) {
			throw new DatoInvalidoException("El stock mínimo no puede ser negativo");
		}
	}

	private void aplicarDatos(Insumo insumo, InsumoRequest request) {
		insumo.setNombre(request.nombre().trim());
		insumo.setDescripcion(request.descripcion());
		insumo.setUnidadMedida(request.unidadMedida().trim());
		insumo.setImagen(request.imagen());
		insumo.setStockMinimo(request.stockMinimo() == null ? 0.0 : request.stockMinimo());
	}

	private Insumo buscarInsumo(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id del insumo es obligatorio");
		}
		return insumoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(MSG_NO_ENCONTRADO + id));
	}

}

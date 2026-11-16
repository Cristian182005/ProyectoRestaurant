package com.elhornero.backend.service;

import com.elhornero.backend.dto.MesaRequest;
import com.elhornero.backend.entity.EstadoPedido;
import com.elhornero.backend.entity.Mesa;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.exception.RecursoNoEncontradoException;
import com.elhornero.backend.repository.MesaRepository;
import com.elhornero.backend.repository.PedidoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MesaService {

	private static final List<EstadoPedido> ESTADOS_ACTIVOS = List.of(EstadoPedido.PENDIENTE,
			EstadoPedido.EN_PREPARACION);

	private final MesaRepository mesaRepository;
	private final PedidoRepository pedidoRepository;

	public MesaService(MesaRepository mesaRepository, PedidoRepository pedidoRepository) {
		this.mesaRepository = mesaRepository;
		this.pedidoRepository = pedidoRepository;
	}

	@Transactional(readOnly = true)
	public List<Mesa> listar() {
		return mesaRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<Mesa> listarPorEstado(String estado) {
		if (estado == null || estado.isBlank()) {
			throw new DatoInvalidoException("El estado de la mesa es obligatorio");
		}
		return mesaRepository.findByEstadoIgnoreCase(estado.trim());
	}

	@Transactional(readOnly = true)
	public Mesa obtener(Integer id) {
		return buscarMesa(id);
	}

	@Transactional
	public Mesa crear(MesaRequest request) {
		validarRequest(request);
		if (mesaRepository.existsByNumero(request.numero())) {
			throw new OperacionNoPermitidaException(
					"Ya existe una mesa con el número " + request.numero());
		}
		Mesa mesa = new Mesa();
		aplicarDatos(mesa, request);
		return mesaRepository.save(mesa);
	}

	@Transactional
	public Mesa actualizar(Integer id, MesaRequest request) {
		validarRequest(request);
		Mesa mesa = buscarMesa(id);
		if (mesaRepository.existsByNumeroAndIdNot(request.numero(), id)) {
			throw new OperacionNoPermitidaException(
					"Ya existe otra mesa con el número " + request.numero());
		}
		aplicarDatos(mesa, request);
		return mesaRepository.save(mesa);
	}

	@Transactional
	public void eliminar(Integer id) {
		Mesa mesa = buscarMesa(id);
		if (pedidoRepository.existsByMesaIdAndEstadoIn(id, ESTADOS_ACTIVOS)) {
			throw new OperacionNoPermitidaException(
					"No se puede eliminar la mesa porque tiene pedidos activos");
		}
		// un pedido historico conserva la referencia a la mesa en la base de datos,
		// por lo que eliminarla violaria la integridad referencial
		if (pedidoRepository.existsByMesaId(id)) {
			throw new OperacionNoPermitidaException(
					"No se puede eliminar la mesa porque tiene pedidos registrados");
		}
		mesaRepository.delete(mesa);
	}

	private void validarRequest(MesaRequest request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos de la mesa son obligatorios");
		}
		if (request.numero() == null) {
			throw new DatoInvalidoException("El número de la mesa es obligatorio");
		}
		if (request.numero() <= 0) {
			throw new DatoInvalidoException("El número de la mesa debe ser mayor a 0");
		}
		if (request.capacidad() == null || request.capacidad() <= 0) {
			throw new DatoInvalidoException("La capacidad de la mesa debe ser mayor a 0");
		}
		if (request.estado() == null || request.estado().isBlank()) {
			throw new DatoInvalidoException("El estado de la mesa es obligatorio");
		}
	}

	private void aplicarDatos(Mesa mesa, MesaRequest request) {
		mesa.setNumero(request.numero());
		mesa.setCapacidad(request.capacidad());
		mesa.setEstado(request.estado().trim());
	}

	private Mesa buscarMesa(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id de la mesa es obligatorio");
		}
		return mesaRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Mesa no encontrada con id " + id));
	}

}

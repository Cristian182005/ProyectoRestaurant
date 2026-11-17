package com.elhornero.backend.service;

import com.elhornero.backend.dto.ItemPedidoRequest;
import com.elhornero.backend.entity.DetallePedido;
import com.elhornero.backend.entity.EstadoPedido;
import com.elhornero.backend.entity.Pedido;
import com.elhornero.backend.entity.Plato;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.exception.RecursoNoEncontradoException;
import com.elhornero.backend.repository.DetallePedidoRepository;
import com.elhornero.backend.repository.PedidoRepository;
import com.elhornero.backend.repository.PlatoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetallePedidoService {

	private final DetallePedidoRepository detallePedidoRepository;
	private final PedidoRepository pedidoRepository;
	private final PlatoRepository platoRepository;

	public DetallePedidoService(DetallePedidoRepository detallePedidoRepository,
			PedidoRepository pedidoRepository, PlatoRepository platoRepository) {
		this.detallePedidoRepository = detallePedidoRepository;
		this.pedidoRepository = pedidoRepository;
		this.platoRepository = platoRepository;
	}

	@Transactional(readOnly = true)
	public List<DetallePedido> listarPorPedido(Integer pedidoId) {
		if (pedidoId == null) {
			throw new DatoInvalidoException("El id del pedido es obligatorio");
		}
		if (!pedidoRepository.existsById(pedidoId)) {
			throw new RecursoNoEncontradoException("Pedido no encontrado con id " + pedidoId);
		}
		return detallePedidoRepository.findByPedidoId(pedidoId);
	}

	@Transactional(readOnly = true)
	public DetallePedido obtener(Integer id) {
		return buscarDetalle(id);
	}

	@Transactional
	public DetallePedido agregarDetalle(Integer pedidoId, ItemPedidoRequest request) {
		if (request == null || request.platoId() == null) {
			throw new DatoInvalidoException("El detalle debe indicar el plato");
		}
		validarCantidad(request.cantidad());
		Pedido pedido = buscarPedidoEditable(pedidoId);
		Plato plato = buscarPlatoActivo(request.platoId());

		DetallePedido existente = detallePedidoRepository.findByPedidoId(pedidoId).stream()
				.filter(d -> d.getPlato().getId().equals(plato.getId()))
				.findFirst()
				.orElse(null);

		if (existente != null) {
			existente.setCantidad(existente.getCantidad() + request.cantidad());
			return detallePedidoRepository.save(existente);
		}

		DetallePedido detalle = new DetallePedido();
		detalle.setPedido(pedido);
		detalle.setPlato(plato);
		detalle.setCantidad(request.cantidad());
		detalle.setPrecioUnitario(plato.getPrecio());
		return detallePedidoRepository.save(detalle);
	}

	@Transactional
	public DetallePedido actualizarCantidad(Integer id, Integer cantidad) {
		validarCantidad(cantidad);
		DetallePedido detalle = buscarDetalle(id);
		Pedido pedido = buscarPedidoEditable(detalle.getPedido().getId());
		detalle.setPedido(pedido);
		detalle.setCantidad(cantidad);
		return detallePedidoRepository.save(detalle);
	}

	@Transactional
	public void eliminar(Integer id) {
		DetallePedido detalle = buscarDetalle(id);
		Pedido pedido = buscarPedidoEditable(detalle.getPedido().getId());
		if (detallePedidoRepository.countByPedidoId(pedido.getId()) <= 1) {
			throw new OperacionNoPermitidaException("El pedido debe tener al menos un detalle");
		}
		detallePedidoRepository.delete(detalle);
	}

	private Pedido buscarPedidoEditable(Integer pedidoId) {
		if (pedidoId == null) {
			throw new DatoInvalidoException("El id del pedido es obligatorio");
		}
		Pedido pedido = pedidoRepository.findById(pedidoId)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Pedido no encontrado con id " + pedidoId));
		if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
			throw new OperacionNoPermitidaException(
					"Los detalles solo pueden modificarse mientras el pedido esté PENDIENTE");
		}
		return pedido;
	}

	private Plato buscarPlatoActivo(Integer id) {
		Plato plato = platoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Plato no encontrado con id " + id));
		if (plato.getActivo() != null && !plato.getActivo()) {
			throw new OperacionNoPermitidaException(
					"El plato '" + plato.getNombre() + "' no está disponible");
		}
		if (plato.getPrecio() == null) {
			throw new OperacionNoPermitidaException(
					"El plato '" + plato.getNombre() + "' no tiene precio registrado");
		}
		return plato;
	}

	private void validarCantidad(Integer cantidad) {
		if (cantidad == null) {
			throw new DatoInvalidoException("La cantidad del detalle es obligatoria");
		}
		if (cantidad <= 0) {
			throw new DatoInvalidoException("La cantidad del detalle debe ser mayor a 0");
		}
	}

	private DetallePedido buscarDetalle(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id del detalle es obligatorio");
		}
		return detallePedidoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Detalle de pedido no encontrado con id " + id));
	}

}

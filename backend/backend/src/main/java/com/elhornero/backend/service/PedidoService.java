package com.elhornero.backend.service;

import com.elhornero.backend.dto.EstadoPedidoRequest;
import com.elhornero.backend.dto.ItemPedidoRequest;
import com.elhornero.backend.dto.PedidoRequest;
import com.elhornero.backend.entity.DetallePedido;
import com.elhornero.backend.entity.Empleado;
import com.elhornero.backend.entity.EstadoPedido;
import com.elhornero.backend.entity.IngredientePlato;
import com.elhornero.backend.entity.Mesa;
import com.elhornero.backend.entity.Pedido;
import com.elhornero.backend.entity.Plato;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.exception.RecursoNoEncontradoException;
import com.elhornero.backend.repository.DetallePedidoRepository;
import com.elhornero.backend.repository.EmpleadoRepository;
import com.elhornero.backend.repository.IngredientePlatoRepository;
import com.elhornero.backend.repository.MesaRepository;
import com.elhornero.backend.repository.PedidoRepository;
import com.elhornero.backend.repository.PlatoRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

	private static final List<EstadoPedido> ESTADOS_ACTIVOS = List.of(EstadoPedido.PENDIENTE,
			EstadoPedido.EN_PREPARACION);

	private final PedidoRepository pedidoRepository;
	private final MesaRepository mesaRepository;
	private final EmpleadoRepository empleadoRepository;
	private final PlatoRepository platoRepository;
	private final DetallePedidoRepository detallePedidoRepository;
	private final IngredientePlatoRepository ingredientePlatoRepository;
	private final InsumoService insumoService;

	public PedidoService(PedidoRepository pedidoRepository, MesaRepository mesaRepository,
			EmpleadoRepository empleadoRepository, PlatoRepository platoRepository,
			DetallePedidoRepository detallePedidoRepository,
			IngredientePlatoRepository ingredientePlatoRepository, InsumoService insumoService) {
		this.pedidoRepository = pedidoRepository;
		this.mesaRepository = mesaRepository;
		this.empleadoRepository = empleadoRepository;
		this.platoRepository = platoRepository;
		this.detallePedidoRepository = detallePedidoRepository;
		this.ingredientePlatoRepository = ingredientePlatoRepository;
		this.insumoService = insumoService;
	}

	@Transactional(readOnly = true)
	public List<Pedido> listar() {
		return pedidoRepository.listarConDetalles();
	}

	@Transactional(readOnly = true)
	public List<Pedido> listarPorEstado(EstadoPedido estado) {
		if (estado == null) {
			throw new DatoInvalidoException("El estado del pedido es obligatorio");
		}
		return pedidoRepository.listarPorEstadoConDetalles(estado);
	}

	@Transactional(readOnly = true)
	public Pedido obtener(Integer id) {
		return buscarPedidoConDetalles(id);
	}

	@Transactional
	public Pedido crear(PedidoRequest request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos del pedido son obligatorios");
		}
		if (request.items() == null || request.items().isEmpty()) {
			throw new DatoInvalidoException("El pedido debe tener al menos un detalle");
		}
		Mesa mesa = buscarMesa(request.mesaId());
		if (pedidoRepository.existsByMesaIdAndEstadoIn(mesa.getId(), ESTADOS_ACTIVOS)) {
			throw new OperacionNoPermitidaException(
					"La mesa " + mesa.getNumero() + " ya tiene un pedido activo");
		}
		Empleado empleado = buscarEmpleado(request.empleadoId());

		Map<Integer, Integer> cantidades = new LinkedHashMap<>();
		for (ItemPedidoRequest item : request.items()) {
			if (item == null || item.platoId() == null) {
				throw new DatoInvalidoException("Cada detalle del pedido debe indicar el plato");
			}
			validarCantidad(item.cantidad());
			cantidades.merge(item.platoId(), item.cantidad(), Integer::sum);
		}

		Pedido pedido = new Pedido();
		pedido.setEstado(EstadoPedido.PENDIENTE);
		pedido.setHoraTomadaDelPedido(LocalDateTime.now());
		pedido.setMesa(mesa);
		pedido.setEmpleado(empleado);

		for (Map.Entry<Integer, Integer> entry : cantidades.entrySet()) {
			Plato plato = buscarPlatoActivo(entry.getKey());
			DetallePedido detalle = new DetallePedido();
			detalle.setPlato(plato);
			detalle.setCantidad(entry.getValue());
			detalle.setPrecioUnitario(plato.getPrecio());
			pedido.agregarDetalle(detalle);
		}
		return pedidoRepository.save(pedido);
	}

	@Transactional
	public Pedido cambiarEstado(Integer id, EstadoPedidoRequest request) {
		if (request == null || request.estado() == null) {
			throw new DatoInvalidoException("El nuevo estado del pedido es obligatorio");
		}
		Pedido pedido = buscarPedidoConDetalles(id);
		EstadoPedido actual = pedido.getEstado();
		EstadoPedido nuevo = request.estado();

		if (actual == nuevo) {
			return pedido;
		}
		validarTransicion(actual, nuevo);

		if (nuevo == EstadoPedido.EN_PREPARACION) {
			descontarInsumosDelPedido(pedido);
		}
		if (nuevo == EstadoPedido.CANCELADO && actual == EstadoPedido.EN_PREPARACION) {
			reponerInsumosDelPedido(pedido);
		}
		pedido.setEstado(nuevo);
		return pedidoRepository.save(pedido);
	}

	@Transactional
	public void eliminar(Integer id) {
		Pedido pedido = buscarPedido(id);
		if (pedido.getEstado() != EstadoPedido.PENDIENTE
				&& pedido.getEstado() != EstadoPedido.CANCELADO) {
			throw new OperacionNoPermitidaException(
					"No se puede eliminar un pedido en estado " + pedido.getEstado());
		}
		pedidoRepository.delete(pedido);
	}

	private void validarTransicion(EstadoPedido actual, EstadoPedido nuevo) {
		boolean valido = switch (actual) {
			case PENDIENTE -> nuevo == EstadoPedido.EN_PREPARACION
					|| nuevo == EstadoPedido.CANCELADO;
			case EN_PREPARACION -> nuevo == EstadoPedido.ENTREGADO
					|| nuevo == EstadoPedido.CANCELADO;
			case ENTREGADO, CANCELADO -> false;
		};
		if (!valido) {
			throw new OperacionNoPermitidaException(
					"No se puede cambiar el pedido de " + actual + " a " + nuevo);
		}
	}

	private void descontarInsumosDelPedido(Pedido pedido) {
		Map<Integer, Double> requerido = calcularInsumosRequeridos(pedido);
		requerido.forEach(insumoService::descontarStock);
	}

	private void reponerInsumosDelPedido(Pedido pedido) {
		Map<Integer, Double> requerido = calcularInsumosRequeridos(pedido);
		requerido.forEach(insumoService::aumentarStock);
	}

	private Map<Integer, Double> calcularInsumosRequeridos(Pedido pedido) {
		Map<Integer, Double> requerido = new LinkedHashMap<>();
		for (DetallePedido detalle : pedido.getDetalles()) {
			List<IngredientePlato> ingredientes = ingredientePlatoRepository
					.findByPlatoId(detalle.getPlato().getId());
			for (IngredientePlato ingrediente : ingredientes) {
				if (ingrediente.getCantidadRequerida() == null
						|| ingrediente.getCantidadRequerida() <= 0) {
					throw new OperacionNoPermitidaException(
							"El plato '" + detalle.getPlato().getNombre()
									+ "' tiene un ingrediente con cantidad inválida");
				}
				double cantidad = ingrediente.getCantidadRequerida() * detalle.getCantidad();
				requerido.merge(ingrediente.getInsumo().getId(), cantidad, Double::sum);
			}
		}
		return requerido;
	}

	private void validarCantidad(Integer cantidad) {
		if (cantidad == null) {
			throw new DatoInvalidoException("La cantidad de cada detalle es obligatoria");
		}
		if (cantidad <= 0) {
			throw new DatoInvalidoException("La cantidad de cada detalle debe ser mayor a 0");
		}
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

	private Mesa buscarMesa(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("La mesa del pedido es obligatoria");
		}
		return mesaRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Mesa no encontrada con id " + id));
	}

	private Empleado buscarEmpleado(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El empleado que toma el pedido es obligatorio");
		}
		return empleadoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Empleado no encontrado con id " + id));
	}

	private Pedido buscarPedido(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id del pedido es obligatorio");
		}
		return pedidoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado con id " + id));
	}

	private Pedido buscarPedidoConDetalles(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id del pedido es obligatorio");
		}
		return pedidoRepository.findByIdConDetalles(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado con id " + id));
	}

}

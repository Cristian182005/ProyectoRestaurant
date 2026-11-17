package com.elhornero.backend.service;

import com.elhornero.backend.dto.ComprobanteRequest;
import com.elhornero.backend.dto.VentasResumenResponse;
import com.elhornero.backend.entity.Boleta;
import com.elhornero.backend.entity.ClienteJuridico;
import com.elhornero.backend.entity.ClienteNatural;
import com.elhornero.backend.entity.Comprobante;
import com.elhornero.backend.entity.DetallePedido;
import com.elhornero.backend.entity.Empleado;
import com.elhornero.backend.entity.EstadoPedido;
import com.elhornero.backend.entity.Factura;
import com.elhornero.backend.entity.Pedido;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.exception.RecursoNoEncontradoException;
import com.elhornero.backend.repository.BoletaRepository;
import com.elhornero.backend.repository.ClienteJuridicoRepository;
import com.elhornero.backend.repository.ClienteNaturalRepository;
import com.elhornero.backend.repository.ComprobanteRepository;
import com.elhornero.backend.repository.EmpleadoRepository;
import com.elhornero.backend.repository.FacturaRepository;
import com.elhornero.backend.repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComprobanteService {

	private static final double IGV = 0.18;

	private final ComprobanteRepository comprobanteRepository;
	private final PedidoRepository pedidoRepository;
	private final EmpleadoRepository empleadoRepository;
	private final ClienteNaturalRepository clienteNaturalRepository;
	private final ClienteJuridicoRepository clienteJuridicoRepository;
	private final BoletaRepository boletaRepository;
	private final FacturaRepository facturaRepository;

	public ComprobanteService(ComprobanteRepository comprobanteRepository,
			PedidoRepository pedidoRepository, EmpleadoRepository empleadoRepository,
			ClienteNaturalRepository clienteNaturalRepository,
			ClienteJuridicoRepository clienteJuridicoRepository, BoletaRepository boletaRepository,
			FacturaRepository facturaRepository) {
		this.comprobanteRepository = comprobanteRepository;
		this.pedidoRepository = pedidoRepository;
		this.empleadoRepository = empleadoRepository;
		this.clienteNaturalRepository = clienteNaturalRepository;
		this.clienteJuridicoRepository = clienteJuridicoRepository;
		this.boletaRepository = boletaRepository;
		this.facturaRepository = facturaRepository;
	}

	@Transactional(readOnly = true)
	public List<Comprobante> listar() {
		return comprobanteRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<Comprobante> listarEntreFechas(LocalDate desde, LocalDate hasta) {
		validarRangoFechas(desde, hasta);
		return comprobanteRepository.listarEntreFechas(desde, hasta);
	}

	@Transactional(readOnly = true)
	public Comprobante obtener(Integer id) {
		return buscarComprobante(id);
	}

	@Transactional(readOnly = true)
	public Comprobante obtenerPorPedido(Integer pedidoId) {
		if (pedidoId == null) {
			throw new DatoInvalidoException("El id del pedido es obligatorio");
		}
		return comprobanteRepository.findByPedidoId(pedidoId)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"El pedido " + pedidoId + " no tiene comprobante emitido"));
	}

	@Transactional
	public Comprobante emitir(ComprobanteRequest request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos del comprobante son obligatorios");
		}
		if (request.tipoPago() == null) {
			throw new DatoInvalidoException("El tipo de pago es obligatorio");
		}
		if (request.montoRecibido() == null || request.montoRecibido() <= 0) {
			throw new DatoInvalidoException("El monto recibido debe ser mayor a 0");
		}
		if (!Double.isFinite(request.montoRecibido())) {
			throw new DatoInvalidoException("El monto recibido no es un valor válido");
		}
		boolean esBoleta = request.clienteNaturalId() != null;
		boolean esFactura = request.clienteJuridicoId() != null;
		if (esBoleta == esFactura) {
			throw new DatoInvalidoException(
					"Debe indicar exactamente un cliente: natural (boleta) o jurídico (factura)");
		}

		// bloqueo pesimista: serializa las emisiones del mismo pedido y evita dos
		// ventas concurrentes antes de validar existByPedidoId
		Pedido pedido = buscarPedidoBloqueado(request.pedidoId());
		if (pedido.getEstado() != EstadoPedido.ENTREGADO) {
			throw new OperacionNoPermitidaException(
					"Solo se puede emitir comprobante de un pedido ENTREGADO");
		}
		if (comprobanteRepository.existsByPedidoId(pedido.getId())) {
			throw new OperacionNoPermitidaException(
					"El pedido " + pedido.getId() + " ya tiene un comprobante emitido");
		}
		Empleado cajero = buscarEmpleado(request.cajeroId());

		double total = calcularTotalPedido(pedido);
		double montoRecibido = request.montoRecibido();
		if (montoRecibido < total) {
			throw new OperacionNoPermitidaException(
					"El monto recibido (" + montoRecibido + ") es menor al total (" + total + ")");
		}

		double gravado = redondear(total / (1 + IGV));
		double igv = redondear(total - gravado);

		Comprobante comprobante = new Comprobante();
		LocalDateTime ahora = LocalDateTime.now();
		comprobante.setFechaEmision(ahora.toLocalDate());
		comprobante.setHoraEmision(ahora.toLocalTime().withNano(0));
		comprobante.setGravado(gravado);
		comprobante.setIgv(igv);
		comprobante.setPrecioTotal(total);
		comprobante.setTipoPago(request.tipoPago());
		comprobante.setMontoRecibido(montoRecibido);
		comprobante.setVueltoPago(redondear(montoRecibido - total));
		comprobante.setCajero(cajero);
		comprobante.setPedido(pedido);

		Comprobante guardado = comprobanteRepository.save(comprobante);

		if (esBoleta) {
			Boleta boleta = new Boleta();
			boleta.setComprobante(guardado);
			boleta.setClienteNatural(buscarClienteNatural(request.clienteNaturalId()));
			boletaRepository.save(boleta);
		} else {
			Factura factura = new Factura();
			factura.setComprobante(guardado);
			factura.setClienteJuridico(buscarClienteJuridico(request.clienteJuridicoId()));
			facturaRepository.save(factura);
		}
		return guardado;
	}

	@Transactional(readOnly = true)
	public VentasResumenResponse resumen(LocalDate desde, LocalDate hasta) {
		validarRangoFechas(desde, hasta);
		List<Comprobante> comprobantes = comprobanteRepository.listarEntreFechas(desde, hasta);
		Double total = comprobanteRepository.sumarTotalEntreFechas(desde, hasta);
		Double totalIgv = comprobanteRepository.sumarIgvEntreFechas(desde, hasta);
		double totalVentas = total == null ? 0.0 : total;
		double igvTotal = totalIgv == null ? 0.0 : totalIgv;
		double promedio = comprobantes.isEmpty() ? 0.0 : redondear(totalVentas / comprobantes.size());
		return new VentasResumenResponse(desde, hasta, comprobantes.size(), redondear(totalVentas),
				redondear(igvTotal), promedio);
	}

	private double calcularTotalPedido(Pedido pedido) {
		List<DetallePedido> detalles = pedidoRepository.findByIdConDetalles(pedido.getId())
				.map(Pedido::getDetalles)
				.orElse(List.of());
		if (detalles.isEmpty()) {
			throw new OperacionNoPermitidaException(
					"El pedido " + pedido.getId() + " no tiene detalles válidos para facturar");
		}
		double total = 0.0;
		for (DetallePedido detalle : detalles) {
			if (detalle.getCantidad() == null || detalle.getCantidad() <= 0
					|| detalle.getPrecioUnitario() == null) {
				throw new OperacionNoPermitidaException(
						"El pedido " + pedido.getId() + " tiene detalles con datos inválidos");
			}
			total += detalle.getCantidad() * detalle.getPrecioUnitario();
		}
		if (total <= 0) {
			throw new OperacionNoPermitidaException(
					"El pedido " + pedido.getId() + " no tiene un total válido para facturar");
		}
		return redondear(total);
	}

	private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
		if (desde == null || hasta == null) {
			throw new DatoInvalidoException("Las fechas del rango son obligatorias");
		}
		if (desde.isAfter(hasta)) {
			throw new DatoInvalidoException("La fecha inicial no puede ser posterior a la final");
		}
	}

	private double redondear(double valor) {
		return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	private Comprobante buscarComprobante(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id del comprobante es obligatorio");
		}
		return comprobanteRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Comprobante no encontrado con id " + id));
	}

	private Pedido buscarPedidoBloqueado(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El pedido del comprobante es obligatorio");
		}
		return pedidoRepository.findByIdParaBloqueo(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado con id " + id));
	}

	private Empleado buscarEmpleado(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El cajero del comprobante es obligatorio");
		}
		return empleadoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Empleado cajero no encontrado con id " + id));
	}

	private ClienteNatural buscarClienteNatural(Integer id) {
		return clienteNaturalRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Cliente natural no encontrado con id " + id));
	}

	private ClienteJuridico buscarClienteJuridico(Integer id) {
		return clienteJuridicoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Cliente jurídico no encontrado con id " + id));
	}

}

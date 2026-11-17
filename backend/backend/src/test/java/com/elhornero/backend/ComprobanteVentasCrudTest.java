package com.elhornero.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elhornero.backend.entity.EstadoPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ComprobanteVentasCrudTest extends BasePruebaIntegracion {

	private int cajeroId;
	private int clienteNaturalId;
	private int clienteJuridicoId;
	private int mesaId;
	private int platoA;
	private int platoB;

	@BeforeEach
	void preparar() throws Exception {
		cajeroId = crearEmpleado(9401, "Cajero").getId();
		clienteNaturalId = crearClienteNatural(9501).getId();
		clienteJuridicoId = crearClienteJuridico(9502).getId();
		mesaId = crearMesa(41);
		platoA = crearPlato("Lomo Saltado", "Platos", 35.0, true);
		platoB = crearPlato("Ceviche", "Platos", 30.0, true);
	}

	@Test
	void emitirComprobanteDeUnPedidoEntregado() throws Exception {
		int pedidoId = pedidoEntregado();

		MvcResult creacion = mockMvc
				.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
						.content(comprobante(pedidoId, cajeroId, "EFECTIVO", 150.0,
								clienteNaturalId, null)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.pedidoId").value(pedidoId))
				.andExpect(jsonPath("$.cajeroId").value(cajeroId))
				.andExpect(jsonPath("$.tipoPago").value("EFECTIVO"))
				.andExpect(jsonPath("$.precioTotal").value(100.0))
				.andExpect(jsonPath("$.gravado").value(84.75))
				.andExpect(jsonPath("$.igv").value(15.25))
				.andExpect(jsonPath("$.montoRecibido").value(150.0))
				.andExpect(jsonPath("$.vueltoPago").value(50.0))
				.andExpect(jsonPath("$.fechaEmision").isNotEmpty())
				.andExpect(jsonPath("$.horaEmision").isNotEmpty()).andReturn();
		int comprobanteId = leerId(creacion);

		// persistencia y relacion con el pedido
		assertThat(comprobanteRepository.findById(comprobanteId)).hasValueSatisfying(
				comprobante -> assertThat(comprobante.getPedido().getId()).isEqualTo(pedidoId));
		assertThat(comprobanteRepository.existsByPedidoId(pedidoId)).isTrue();
		assertThat(comprobanteRepository.findByPedidoId(pedidoId)).isPresent();

		// READ por id y por pedido
		mockMvc.perform(get("/api/comprobantes/" + comprobanteId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(comprobanteId));
		mockMvc.perform(get("/api/comprobantes/pedido/" + pedidoId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(comprobanteId));
		mockMvc.perform(get("/api/comprobantes")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/api/comprobantes").param("desde", "2000-01-01")
				.param("hasta", "2100-12-31")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/api/comprobantes").param("desde", "2000-01-01")
				.param("hasta", "2000-01-02")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void emitirFacturaParaClienteJuridico() throws Exception {
		int pedidoId = pedidoEntregado();

		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, cajeroId, "YAPE", 100.0, null,
						clienteJuridicoId)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.tipoPago").value("YAPE"))
				.andExpect(jsonPath("$.pedidoId").value(pedidoId))
				.andExpect(jsonPath("$.vueltoPago").value(0.0));

		assertThat(facturaRepository.count()).isEqualTo(1);
		assertThat(boletaRepository.count()).isZero();
	}

	@Test
	void noSePuedeEmitirUnSegundoComprobanteMismoPedido() throws Exception {
		int pedidoId = pedidoEntregado();
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, cajeroId, "EFECTIVO", 100.0, clienteNaturalId,
						null)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, cajeroId, "TARJETA", 100.0, clienteNaturalId,
						null)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(
						"El pedido " + pedidoId + " ya tiene un comprobante emitido"));

		assertThat(comprobanteRepository.count()).isEqualTo(1);
	}

	@Test
	void rechazaComprobantesInvalidos() throws Exception {
		int pedidoId = pedidoEntregado();

		// pedido que no esta entregado
		int pedidoPendiente = crearPedidoPendiente();
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoPendiente, cajeroId, "EFECTIVO", 100.0,
						clienteNaturalId, null)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje")
						.value("Solo se puede emitir comprobante de un pedido ENTREGADO"));

		// monto insuficiente
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, cajeroId, "EFECTIVO", 50.0, clienteNaturalId,
						null)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(
						"El monto recibido (50.0) es menor al total (100.0)"));

		// sin tipo de pago
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"pedidoId\":" + pedidoId + ",\"cajeroId\":" + cajeroId
						+ ",\"montoRecibido\":100,\"clienteNaturalId\":" + clienteNaturalId + "}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El tipo de pago es obligatorio"));

		// sin cliente
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, cajeroId, "EFECTIVO", 100.0, null, null)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value(
						"Debe indicar exactamente un cliente: natural (boleta) o jurídico (factura)"));

		// con los dos clientes
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, cajeroId, "EFECTIVO", 100.0, clienteNaturalId,
						clienteJuridicoId)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value(
						"Debe indicar exactamente un cliente: natural (boleta) o jurídico (factura)"));

		// monto nulo o negativo
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, cajeroId, "EFECTIVO", null, clienteNaturalId,
						null)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El monto recibido debe ser mayor a 0"));

		// cliente inexistente
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, cajeroId, "EFECTIVO", 100.0, 999999, null)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.mensaje")
						.value("Cliente natural no encontrado con id 999999"));

		// pedido inexistente
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(999999, cajeroId, "EFECTIVO", 100.0, clienteNaturalId,
						null)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Pedido no encontrado con id 999999"));

		// rango de fechas invalido
		mockMvc.perform(get("/api/comprobantes/resumen").param("desde", "2026-12-31")
				.param("hasta", "2026-01-01")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("La fecha inicial no puede ser posterior a la final"));
		mockMvc.perform(get("/api/comprobantes/resumen")).andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/comprobantes/999999")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/comprobantes/pedido/999999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje")
						.value("El pedido 999999 no tiene comprobante emitido"));

		assertThat(comprobanteRepository.count()).isZero();
		assertThat(boletaRepository.count()).isZero();
		assertThat(facturaRepository.count()).isZero();
	}

	@Test
	void elResumenDeVentasAgrupaLosComprobantes() throws Exception {
		int primero = pedidoEntregado();
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(primero, cajeroId, "EFECTIVO", 150.0, clienteNaturalId,
						null)))
				.andExpect(status().isCreated());

		int segundo = pedidoEntregado();
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(segundo, cajeroId, "TARJETA", 100.0, null,
						clienteJuridicoId)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.tipoPago").value("TARJETA"))
				.andExpect(jsonPath("$.pedidoId").value(segundo));

		mockMvc.perform(get("/api/comprobantes/resumen").param("desde", "2000-01-01")
				.param("hasta", "2100-12-31")).andExpect(status().isOk())
				.andExpect(jsonPath("$.cantidadComprobantes").value(2))
				.andExpect(jsonPath("$.totalVentas").value(200.0))
				.andExpect(jsonPath("$.totalIgv").value(30.5))
				.andExpect(jsonPath("$.promedioTicket").value(100.0))
				.andExpect(jsonPath("$.desde").value("2000-01-01"))
				.andExpect(jsonPath("$.hasta").value("2100-12-31"));

		mockMvc.perform(get("/api/comprobantes/resumen").param("desde", "2000-01-01")
				.param("hasta", "2000-01-02")).andExpect(status().isOk())
				.andExpect(jsonPath("$.cantidadComprobantes").value(0))
				.andExpect(jsonPath("$.totalVentas").value(0.0))
				.andExpect(jsonPath("$.promedioTicket").value(0.0));

		// el pedido conserva su estado entregado
		assertThat(pedidoRepository.findById(primero)).hasValueSatisfying(
				pedido -> assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO));
	}

	@Test
	void rechazaComprobantesConCajeroOClienteInexistente() throws Exception {
		int pedidoId = pedidoEntregado();

		// el cajero se resuelve despues de validar el pedido: 404 y nada persiste
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, 999999, "EFECTIVO", 100.0, clienteNaturalId,
						null)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.mensaje")
						.value("Empleado cajero no encontrado con id 999999"));
		assertThat(comprobanteRepository.count()).isZero();

		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, cajeroId, "EFECTIVO", 100.0, null, 999999)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.mensaje")
						.value("Cliente jurídico no encontrado con id 999999"));
		assertThat(comprobanteRepository.count()).isZero();
		assertThat(facturaRepository.count()).isZero();
		assertThat(boletaRepository.count()).isZero();
	}

	@Test
	void rechazaTiposDePagoYMontoMalFormados() throws Exception {
		int pedidoId = pedidoEntregado();

		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, cajeroId, "BITCOIN", 100.0, clienteNaturalId,
						null)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El cuerpo de la solicitud no tiene un formato valido"));
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"pedidoId\":" + pedidoId + ",\"cajeroId\":" + cajeroId
						+ ",\"tipoPago\":\"EFECTIVO\",\"montoRecibido\":\"abc\","
						+ "\"clienteNaturalId\":" + clienteNaturalId + "}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El cuerpo de la solicitud no tiene un formato valido"));

		assertThat(comprobanteRepository.count()).isZero();
		assertThat(pedidoRepository.findById(pedidoId)).hasValueSatisfying(
				pedido -> assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO));
	}

	@Test
	void rechazaRangosDeFechaIncompletosOInvalidos() throws Exception {
		mockMvc.perform(get("/api/comprobantes").param("desde", "2000-01-01"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("Las fechas del rango son obligatorias"));
		mockMvc.perform(get("/api/comprobantes").param("hasta", "2100-12-31"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("Las fechas del rango son obligatorias"));
		mockMvc.perform(get("/api/comprobantes").param("desde", "2026-13-45")
				.param("hasta", "2026-12-31")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("El parametro 'desde' tiene un valor invalido"));
		mockMvc.perform(get("/api/comprobantes/resumen").param("desde", "no-es-fecha")
				.param("hasta", "2026-12-31")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("El parametro 'desde' tiene un valor invalido"));
	}

	private int pedidoEntregado() throws Exception {
		int pedidoId = crearPedidoPendiente();
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENTREGADO\"}"))
				.andExpect(status().isOk());
		return pedidoId;
	}

	private int crearPedidoPendiente() throws Exception {
		String cuerpo = "{\"mesaId\":" + mesaId + ",\"empleadoId\":" + cajeroId
				+ ",\"items\":[{\"platoId\":" + platoA + ",\"cantidad\":2},{\"platoId\":" + platoB
				+ ",\"cantidad\":1}]}";
		return leerId(mockMvc
				.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isCreated()).andReturn());
	}

	private String comprobante(Integer pedidoId, Integer cajeroId, String tipoPago,
			Double montoRecibido, Integer clienteNaturalId, Integer clienteJuridicoId) {
		return "{\"pedidoId\":" + pedidoId + ",\"cajeroId\":" + cajeroId + ",\"tipoPago\":\""
				+ tipoPago + "\",\"montoRecibido\":"
				+ (montoRecibido == null ? "null" : montoRecibido) + ",\"clienteNaturalId\":"
				+ clienteNaturalId + ",\"clienteJuridicoId\":" + clienteJuridicoId + "}";
	}

}

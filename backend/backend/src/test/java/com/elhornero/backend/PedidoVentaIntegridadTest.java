package com.elhornero.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elhornero.backend.dto.ComprobanteRequest;
import com.elhornero.backend.entity.EstadoPedido;
import com.elhornero.backend.entity.TipoPago;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.service.ComprobanteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Refuerza la integridad del registro de pedidos y ventas: pedidos siempre con
 * detalle valido, importes calculados, una sola venta por pedido y un unico
 * descuento de inventario por ciclo de vida.
 */
class PedidoVentaIntegridadTest extends BasePruebaIntegracion {

	@Autowired
	private ComprobanteService comprobanteService;

	private int empleadoId;
	private int clienteNaturalId;
	private int mesaId;
	private int platoA;
	private int platoB;

	@BeforeEach
	void preparar() throws Exception {
		empleadoId = crearEmpleado(9601, "Cajero").getId();
		clienteNaturalId = crearClienteNatural(9602).getId();
		mesaId = crearMesa(51);
		platoA = crearPlato("Lomo Saltado", "Platos", 35.0, true);
		platoB = crearPlato("Ceviche", "Platos", 30.0, true);
	}

	@Test
	void noPermiteEliminarElUltimoDetalleDelPedido() throws Exception {
		int pedidoId = crearPedido(platoA, 1);
		int detalleId = detallePedidoRepository.findByPedidoId(pedidoId).get(0).getId();

		// con un solo detalle no se puede borrar: el pedido quedaria vacio
		mockMvc.perform(delete("/api/detalles-pedido/" + detalleId)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje")
						.value("El pedido debe tener al menos un detalle"));
		assertThat(detallePedidoRepository.findByPedidoId(pedidoId)).hasSize(1);

		// con dos detalles si se puede eliminar uno
		mockMvc.perform(post("/api/pedidos/" + pedidoId + "/detalles")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoB + ",\"cantidad\":1}"))
				.andExpect(status().isCreated());
		mockMvc.perform(delete("/api/detalles-pedido/" + detalleId))
				.andExpect(status().isNoContent());
		assertThat(detallePedidoRepository.findByPedidoId(pedidoId)).hasSize(1);

		// pero nunca se llega a cero
		int restante = detallePedidoRepository.findByPedidoId(pedidoId).get(0).getId();
		mockMvc.perform(delete("/api/detalles-pedido/" + restante))
				.andExpect(status().isConflict());
		assertThat(detallePedidoRepository.findByPedidoId(pedidoId)).hasSize(1);
	}

	@Test
	void elPedidoExponeElTotalCalculadoDesdeSusDetalles() throws Exception {
		MvcResult creacion = mockMvc
				.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
						.content(pedido(platoA, 2, platoB, 1)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(100.0))
				.andExpect(jsonPath("$.detalles.length()").value(2)).andReturn();
		int pedidoId = leerId(creacion);

		// 2 * 35.0 + 1 * 30.0 = 100.0
		mockMvc.perform(get("/api/pedidos/" + pedidoId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(100.0))
				.andExpect(jsonPath("$.detalles[0].subtotal").isNumber());

		mockMvc.perform(get("/api/pedidos")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].total").value(100.0));

		// al agregar otro detalle el total se recalcula: 100.0 + 15.0
		int platoC = crearPlato("Entrada", "Entradas", 15.0, true);
		mockMvc.perform(post("/api/pedidos/" + pedidoId + "/detalles")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoC + ",\"cantidad\":1}"))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/pedidos/" + pedidoId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(115.0));
	}

	@Test
	void emitirVentaRechazaMontosNoFinitos() throws Exception {
		int pedidoId = pedidoEntregado();

		// 1e999 se desborda a Infinity al deserializar: no puede facturarse
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"pedidoId\":" + pedidoId + ",\"cajeroId\":" + empleadoId
						+ ",\"tipoPago\":\"EFECTIVO\",\"montoRecibido\":1e999,\"clienteNaturalId\":"
						+ clienteNaturalId + "}"))
				.andExpect(status().isBadRequest());

		assertThat(comprobanteRepository.count()).isZero();
		assertThat(boletaRepository.count()).isZero();

		// la validacion finita vive en el servicio (cubre el caso NaN/Infinity)
		ComprobanteRequest infinito = new ComprobanteRequest(pedidoId, empleadoId,
				TipoPago.EFECTIVO, Double.POSITIVE_INFINITY, clienteNaturalId, null);
		assertThatThrownBy(() -> comprobanteService.emitir(infinito))
				.isInstanceOf(DatoInvalidoException.class)
				.hasMessage("El monto recibido no es un valor válido");

		assertThat(comprobanteRepository.count()).isZero();
	}

	@Test
	void laVentaNoDuplicaElPedidoNiElDescuentoDeStock() throws Exception {
		int insumoId = crearInsumo("Harina Venta", 10.0, 1.0);
		int platoConInsumo = crearPlato("Plato Venta", "Platos", 40.0, true);
		mockMvc.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoConInsumo + ",\"insumoId\":" + insumoId
						+ ",\"cantidadRequerida\":2}"))
				.andExpect(status().isCreated());

		int pedidoId = crearPedido(platoConInsumo, 2);

		// EN_PREPARACION descuenta una sola vez: 10 - (2 * 2) = 6
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isOk());
		double stockTrasPreparacion = insumoRepository.findById(insumoId).orElseThrow()
				.getStockActual();
		assertThat(stockTrasPreparacion).isEqualTo(6.0);

		// repetir el mismo estado no vuelve a descontar (el mismo estado es no-op)
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("EN_PREPARACION"));
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"PENDIENTE\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(
						"No se puede cambiar el pedido de EN_PREPARACION a PENDIENTE"));
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(6.0));

		// ENTREGADO
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENTREGADO\"}"))
				.andExpect(status().isOk());
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(6.0));

		// la venta no toca el stock: sigue descontado solo una vez
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, "EFECTIVO", 100.0)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.pedidoId").value(pedidoId));
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(stockTrasPreparacion));
		assertThat(pedidoRepository.findById(pedidoId)).hasValueSatisfying(
				pedido -> assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO));

		// una sola venta por pedido
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content(comprobante(pedidoId, "TARJETA", 100.0)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(
						"El pedido " + pedidoId + " ya tiene un comprobante emitido"));
		assertThat(comprobanteRepository.count()).isEqualTo(1);
		assertThat(boletaRepository.count()).isEqualTo(1);
		assertThat(facturaRepository.count()).isZero();
	}

	@Test
	void elTotalSeRecalculaAlEditarUnDetalleExistente() throws Exception {
		MvcResult creacion = mockMvc
				.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
						.content(pedido(platoA, 2, platoB, 1)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(100.0))
				.andReturn();
		int pedidoId = leerId(creacion);
		int detalleId = detallePedidoRepository.findByPedidoId(pedidoId).get(0).getId();

		// 2 * 35.0 = 70.0 en el primer detalle; al pasarlo a 3 unidades el total
		// del pedido debe reflejar el nuevo importe
		mockMvc.perform(put("/api/detalles-pedido/" + detalleId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":3}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.cantidad").value(3))
				.andExpect(jsonPath("$.subtotal").value(105.0));

		// 3 * 35.0 + 1 * 30.0 = 135.0 persistido en PostgreSQL
		mockMvc.perform(get("/api/pedidos/" + pedidoId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(135.0));
		assertThat(detallePedidoRepository.findById(detalleId)).hasValueSatisfying(
				detalle -> assertThat(detalle.getCantidad()).isEqualTo(3));
	}

	private int pedidoEntregado() throws Exception {
		int pedidoId = crearPedido(platoA, 2);
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENTREGADO\"}"))
				.andExpect(status().isOk());
		return pedidoId;
	}

	private int crearPedido(int plato, int cantidad) throws Exception {
		return leerId(crearPedido(plato, cantidad, null, 0));
	}

	private MvcResult crearPedido(int platoAId, int cantidadA, Integer platoBId, int cantidadB)
			throws Exception {
		String items = "{\"platoId\":" + platoAId + ",\"cantidad\":" + cantidadA + "}";
		if (platoBId != null) {
			items += ",{\"platoId\":" + platoBId + ",\"cantidad\":" + cantidadB + "}";
		}
		return mockMvc
				.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
						.content("{\"mesaId\":" + mesaId + ",\"empleadoId\":" + empleadoId
								+ ",\"items\":[" + items + "]}"))
				.andExpect(status().isCreated()).andReturn();
	}

	private String pedido(int platoAId, int cantidadA, int platoBId, int cantidadB) {
		return "{\"mesaId\":" + mesaId + ",\"empleadoId\":" + empleadoId + ",\"items\":[{\"platoId\":"
				+ platoAId + ",\"cantidad\":" + cantidadA + "},{\"platoId\":" + platoBId
				+ ",\"cantidad\":" + cantidadB + "}]}";
	}

	private String comprobante(int pedidoId, String tipoPago, double montoRecibido) {
		return "{\"pedidoId\":" + pedidoId + ",\"cajeroId\":" + empleadoId + ",\"tipoPago\":\""
				+ tipoPago + "\",\"montoRecibido\":" + montoRecibido + ",\"clienteNaturalId\":"
				+ clienteNaturalId + "}";
	}

}

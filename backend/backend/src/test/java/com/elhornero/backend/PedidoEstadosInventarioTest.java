package com.elhornero.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elhornero.backend.entity.EstadoPedido;
import com.elhornero.backend.entity.IngredientePlato;
import com.elhornero.backend.entity.Insumo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Comprueba el ciclo de vida del pedido y que el inventario de insumos se
 * actualice de forma transaccional (todo o nada) al cambiar de estado.
 */
class PedidoEstadosInventarioTest extends BasePruebaIntegracion {

	private int empleadoId;
	private int mesaId;
	private int platoId;
	private int insumoId;

	@BeforeEach
	void preparar() throws Exception {
		empleadoId = crearEmpleado(9301, "Cocinero").getId();
		mesaId = crearMesa(31);
		insumoId = crearInsumo("Harina Pedido", 10.0, 2.0);
		platoId = crearPlato("Plato con insumo", "Platos", 25.0, true);
		mockMvc.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoId + ",\"insumoId\":" + insumoId
						+ ",\"cantidadRequerida\":2}"))
				.andExpect(status().isCreated());
	}

	@Test
	void elPasoAEnPreparacionDescuentaElStockDelInventario() throws Exception {
		int pedidoId = crearPedido(2);

		// sin cambios antes de la preparacion
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(10.0));

		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("EN_PREPARACION"));

		// 10 - (2 unidades * 2 por plato) = 6
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(6.0));

		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENTREGADO\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ENTREGADO"));
		assertThat(pedidoRepository.findById(pedidoId)).hasValueSatisfying(
				pedido -> assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO));
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(6.0));
	}

	@Test
	void elStockInsuficienteRevTodaLaTransaccion() throws Exception {
		// stock disponible 3 y el pedido pide 2 unidades * 2 por plato = 4
		Insumo existente = insumoRepository.findById(insumoId).orElseThrow();
		existente.setStockActual(3.0);
		insumoRepository.save(existente);
		int pedidoId = crearPedido(2);

		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value(
						"Stock insuficiente de 'Harina Pedido': disponible 3.0, requerido 4.0"));

		// ninguna de las dos tablas quedo modificada
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(3.0));
		assertThat(pedidoRepository.findById(pedidoId)).hasValueSatisfying(
				pedido -> assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE));
	}

	@Test
	void cancelarEnPreparacionReponeElStock() throws Exception {
		int pedidoId = crearPedido(1);

		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isOk());
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(8.0));

		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADO\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADO"));
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(10.0));
		assertThat(pedidoRepository.findById(pedidoId)).hasValueSatisfying(
				pedido -> assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO));
	}

	@Test
	void soloAceptaTransicionesDeEstadoValidas() throws Exception {
		int pedidoId = crearPedido(1);

		// PENDIENTE -> ENTREGADO no valido
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENTREGADO\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(
						"No se puede cambiar el pedido de PENDIENTE a ENTREGADO"));

		// el mismo estado no genera cambios
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"PENDIENTE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"));

		// PENDIENTE -> EN_PREPARACION -> ENTREGADO -> CANCELADO ya no es valido
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENTREGADO\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADO\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(
						"No se puede cambiar el pedido de ENTREGADO a CANCELADO"));

		// estado invalido en el cuerpo
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CUALQUIERA\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/api/pedidos/999999/estado").contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"CANCELADO\"}")).andExpect(status().isNotFound());
	}

	@Test
	void unIngredienteConCantidadInvalidaImpidePrepararElPedido() throws Exception {
		// se fuerza un ingrediente invalido directamente en base (la API lo rechaza)
		IngredientePlato ingrediente = ingredientePlatoRepository
				.findByPlatoIdAndInsumoId(platoId, insumoId).orElseThrow();
		ingrediente.setCantidadRequerida(0.0);
		ingredientePlatoRepository.save(ingrediente);

		int pedidoId = crearPedido(1);
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value(
						"El plato 'Plato con insumo' tiene un ingrediente con cantidad inválida"));

		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(10.0));
		assertThat(pedidoRepository.findById(pedidoId)).hasValueSatisfying(
				pedido -> assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE));
	}

	@Test
	void elDescuentoParcialDeVariosInsumosSeRevuelveCompleto() throws Exception {
		// dos insumos para el mismo plato: el primero tiene stock suficiente y el
		// segundo no, de modo que al fallar el segundo debe revertirse el primero
		int insumoSecundario = crearInsumo("Levadura Pedido", 50.0, 5.0);
		mockMvc.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoId + ",\"insumoId\":" + insumoSecundario
						+ ",\"cantidadRequerida\":1}"))
				.andExpect(status().isCreated());
		// el insumo principal solo alcanza para 1 unidad del pedido (2 requeridas)
		Insumo principal = insumoRepository.findById(insumoId).orElseThrow();
		principal.setStockActual(3.0);
		insumoRepository.save(principal);

		int pedidoId = crearPedido(2);

		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value(
						"Stock insuficiente de 'Harina Pedido': disponible 3.0, requerido 4.0"));

		// ninguna fila quedo modificada: el descuento previo se revirtio
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(3.0));
		assertThat(insumoRepository.findById(insumoSecundario)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(50.0));
		assertThat(pedidoRepository.findById(pedidoId)).hasValueSatisfying(
				pedido -> assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE));
	}

	@Test
	void elListadoPorEstadoReflejaLosCambios() throws Exception {
		int pedidoId = crearPedido(1);
		mockMvc.perform(get("/api/pedidos").param("estado", "PENDIENTE"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/pedidos").param("estado", "PENDIENTE"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/pedidos").param("estado", "EN_PREPARACION"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(pedidoId));
	}

	private int crearPedido(int cantidad) throws Exception {
		String cuerpo = "{\"mesaId\":" + mesaId + ",\"empleadoId\":" + empleadoId
				+ ",\"items\":[{\"platoId\":" + platoId + ",\"cantidad\":" + cantidad + "}]}";
		return leerId(mockMvc
				.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isCreated()).andReturn());
	}

}

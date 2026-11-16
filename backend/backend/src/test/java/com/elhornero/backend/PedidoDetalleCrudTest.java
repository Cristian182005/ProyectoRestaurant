package com.elhornero.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

class PedidoDetalleCrudTest extends BasePruebaIntegracion {

	private int empleadoId;
	private int platoId;
	private int mesaId;

	@BeforeEach
	void preparar() {
		empleadoId = crearEmpleado(9201, "Mesero").getId();
	}

	@Test
	void registrarPedidoConSusDetalles() throws Exception {
		platoId = crearPlato("Lomo Saltado", "Platos", 35.0, true);
		int platoB = crearPlato("Ceviche", "Platos", 30.0, true);
		mesaId = crearMesa(21);

		MvcResult creacion = mockMvc
				.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
						.content(pedido(mesaId, empleadoId, platoId, 2, platoB, 1)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.mesaId").value(mesaId))
				.andExpect(jsonPath("$.mesaNumero").value(21))
				.andExpect(jsonPath("$.empleadoId").value(empleadoId))
				.andExpect(jsonPath("$.horaTomadaDelPedido").isNotEmpty())
				.andExpect(jsonPath("$.detalles.length()").value(2)).andReturn();
		int pedidoId = leerId(creacion);

		// persistencia de cabecera + detalles en PostgreSQL
		assertThat(pedidoRepository.findByIdConDetalles(pedidoId)).hasValueSatisfying(pedido -> {
			assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
			assertThat(pedido.getDetalles()).hasSize(2);
			assertThat(pedido.getDetalles())
					.allSatisfy(detalle -> assertThat(detalle.getPrecioUnitario()).isPositive());
		});

		// READ por id y de los detalles
		mockMvc.perform(get("/api/pedidos/" + pedidoId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(pedidoId))
				.andExpect(jsonPath("$.detalles.length()").value(2));
		mockMvc.perform(get("/api/pedidos/" + pedidoId + "/detalles"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].subtotal").isNumber())
				.andExpect(jsonPath("$[?(@.platoNombre == 'Lomo Saltado')]").exists())
				.andExpect(jsonPath("$[?(@.platoNombre == 'Ceviche')]").exists());

		// READ listado general y por estado
		mockMvc.perform(get("/api/pedidos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/api/pedidos").param("estado", "PENDIENTE"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/api/pedidos").param("estado", "ENTREGADO"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void rechazaPedidosInvalidos() throws Exception {
		platoId = crearPlato("Plato Valido", "Platos", 20.0, true);
		mesaId = crearMesa(22);

		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"mesaId\":" + mesaId + ",\"empleadoId\":" + empleadoId
						+ ",\"items\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El pedido debe tener al menos un detalle"));

		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"empleadoId\":" + empleadoId + ",\"items\":[{\"platoId\":" + platoId
						+ ",\"cantidad\":1}]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La mesa del pedido es obligatoria"));

		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"mesaId\":" + mesaId + ",\"items\":[{\"platoId\":" + platoId
						+ ",\"cantidad\":1}]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("El empleado que toma el pedido es obligatorio"));

		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content(pedido(mesaId, empleadoId, platoId, 0)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("La cantidad de cada detalle debe ser mayor a 0"));

		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content(pedido(999999, empleadoId, platoId, 1)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Mesa no encontrada con id 999999"));

		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content(pedido(mesaId, 999999, platoId, 1)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Empleado no encontrado con id 999999"));

		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content(pedido(mesaId, empleadoId, 999999, 1)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Plato no encontrado con id 999999"));

		int platoInactivo = crearPlato("Plato Baja", "Platos", 10.0, false);
		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content(pedido(mesaId, empleadoId, platoInactivo, 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje")
						.value("El plato 'Plato Baja' no está disponible"));

		assertThat(pedidoRepository.count()).isZero();
	}

	@Test
	void noPermiteDosPedidosActivosEnLaMismaMesa() throws Exception {
		platoId = crearPlato("Plato Mesa", "Platos", 25.0, true);
		mesaId = crearMesa(23);

		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content(pedido(mesaId, empleadoId, platoId, 1)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content(pedido(mesaId, empleadoId, platoId, 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje")
						.value("La mesa 23 ya tiene un pedido activo"));

		assertThat(pedidoRepository.count()).isEqualTo(1);
	}

	@Test
	void crudDeLosDetallesDelPedido() throws Exception {
		platoId = crearPlato("Plato Detalle", "Platos", 40.0, true);
		int platoB = crearPlato("Plato Extra", "Platos", 15.0, true);
		mesaId = crearMesa(24);

		int pedidoId = crearPedido(mesaId, empleadoId, platoId, 1);

		// CREATE de un detalle nuevo
		MvcResult creacion = mockMvc
				.perform(post("/api/pedidos/" + pedidoId + "/detalles")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"platoId\":" + platoB + ",\"cantidad\":3}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.platoNombre").value("Plato Extra"))
				.andExpect(jsonPath("$.cantidad").value(3))
				.andExpect(jsonPath("$.precioUnitario").value(15.0))
				.andExpect(jsonPath("$.subtotal").value(45.0)).andReturn();
		int detalleId = leerId(creacion);
		assertThat(detallePedidoRepository.findByPedidoId(pedidoId)).hasSize(2);

		// CREATE que suma sobre el detalle existente del mismo plato
		mockMvc.perform(post("/api/pedidos/" + pedidoId + "/detalles")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoB + ",\"cantidad\":2}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.cantidad").value(5));
		assertThat(detallePedidoRepository.findByPedidoId(pedidoId)).hasSize(2);

		// READ del detalle
		mockMvc.perform(get("/api/detalles-pedido/" + detalleId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(detalleId))
				.andExpect(jsonPath("$.pedidoId").value(pedidoId));

		// UPDATE de la cantidad
		mockMvc.perform(put("/api/detalles-pedido/" + detalleId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":7}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.cantidad").value(7));
		assertThat(detallePedidoRepository.findById(detalleId)).hasValueSatisfying(
				detalle -> assertThat(detalle.getCantidad()).isEqualTo(7));

		// DELETE del detalle
		mockMvc.perform(delete("/api/detalles-pedido/" + detalleId))
				.andExpect(status().isNoContent());
		assertThat(detallePedidoRepository.findById(detalleId)).isEmpty();

		// validaciones
		mockMvc.perform(put("/api/detalles-pedido/" + detalleId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":0}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/detalles-pedido/999999")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/pedidos/999999/detalles")).andExpect(status().isNotFound());
	}

	@Test
	void noPermiteModificarDetallesFueraDeEstadoPendiente() throws Exception {
		platoId = crearPlato("Plato Fijo", "Platos", 30.0, true);
		mesaId = crearMesa(25);
		int pedidoId = crearPedido(mesaId, empleadoId, platoId, 1);
		int detalleId = detallePedidoRepository.findByPedidoId(pedidoId).get(0).getId();

		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/pedidos/" + pedidoId + "/detalles")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoId + ",\"cantidad\":1}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(
						"Los detalles solo pueden modificarse mientras el pedido esté PENDIENTE"));

		mockMvc.perform(put("/api/detalles-pedido/" + detalleId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":9}"))
				.andExpect(status().isConflict());
		mockMvc.perform(delete("/api/detalles-pedido/" + detalleId))
				.andExpect(status().isConflict());

		assertThat(detallePedidoRepository.findById(detalleId)).hasValueSatisfying(
				detalle -> assertThat(detalle.getCantidad()).isEqualTo(1));
	}

	@Test
	void eliminarPedidoRespetaElEstadoYLasRelaciones() throws Exception {
		platoId = crearPlato("Plato Borrar", "Platos", 20.0, true);
		mesaId = crearMesa(26);
		int pedidoId = crearPedido(mesaId, empleadoId, platoId, 1);

		// un pedido en preparacion no se puede eliminar
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/pedidos/" + pedidoId)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje")
						.value("No se puede eliminar un pedido en estado EN_PREPARACION"));

		// cancelado si se puede
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADO\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/pedidos/" + pedidoId)).andExpect(status().isNoContent());
		assertThat(pedidoRepository.findById(pedidoId)).isEmpty();
		assertThat(detallePedidoRepository.findByPedidoId(pedidoId)).isEmpty();
	}

	@Test
	void crudDeMesasYReglasDeRelacion() throws Exception {
		// CREATE
		MvcResult creacion = mockMvc
				.perform(post("/api/mesas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"numero\":77,\"estado\":\"LIBRE\",\"capacidad\":4}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.numero").value(77))
				.andExpect(jsonPath("$.estado").value("LIBRE"))
				.andExpect(jsonPath("$.capacidad").value(4)).andReturn();
		int mesaIdLocal = leerId(creacion);
		assertThat(mesaRepository.findById(mesaIdLocal)).isPresent();

		// READ
		mockMvc.perform(get("/api/mesas/" + mesaIdLocal)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(mesaIdLocal));
		mockMvc.perform(get("/api/mesas")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/api/mesas").param("estado", "LIBRE")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		// UPDATE
		mockMvc.perform(put("/api/mesas/" + mesaIdLocal).contentType(MediaType.APPLICATION_JSON)
				.content("{\"numero\":78,\"estado\":\"OCUPADA\",\"capacidad\":6}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.numero").value(78))
				.andExpect(jsonPath("$.estado").value("OCUPADA"));
		assertThat(mesaRepository.findById(mesaIdLocal)).hasValueSatisfying(
				mesa -> assertThat(mesa.getCapacidad()).isEqualTo(6));

		// numero duplicado
		mockMvc.perform(post("/api/mesas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"numero\":78,\"estado\":\"LIBRE\",\"capacidad\":2}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje")
						.value("Ya existe una mesa con el número 78"));

		// validaciones
		mockMvc.perform(post("/api/mesas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"numero\":0,\"estado\":\"LIBRE\",\"capacidad\":4}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/mesas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"numero\":79,\"estado\":\"LIBRE\",\"capacidad\":0}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/mesas/999999")).andExpect(status().isNotFound());

		// no se puede eliminar una mesa con pedido activo
		platoId = crearPlato("Plato Mesa Relacionada", "Platos", 20.0, true);
		int pedidoId = crearPedido(mesaIdLocal, empleadoId, platoId, 1);
		mockMvc.perform(delete("/api/mesas/" + mesaIdLocal)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje")
						.value("No se puede eliminar la mesa porque tiene pedidos activos"));

		// aunque el pedido este terminado, la mesa conserva pedidos registrados
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/pedidos/" + pedidoId + "/estado")
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENTREGADO\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/mesas/" + mesaIdLocal)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje")
						.value("No se puede eliminar la mesa porque tiene pedidos registrados"));
		assertThat(mesaRepository.findById(mesaIdLocal)).isPresent();

		// una mesa sin pedidos si se puede eliminar
		int mesaLibre = crearMesa(79);
		mockMvc.perform(delete("/api/mesas/" + mesaLibre)).andExpect(status().isNoContent());
		assertThat(mesaRepository.findById(mesaLibre)).isEmpty();
	}

	private int crearPedido(int mesa, int empleado, int plato, int cantidad) throws Exception {
		return leerId(mockMvc
				.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
						.content(pedido(mesa, empleado, plato, cantidad)))
				.andExpect(status().isCreated()).andReturn());
	}

	private String pedido(int mesa, int empleado, int plato, int cantidad) {
		return "{\"mesaId\":" + mesa + ",\"empleadoId\":" + empleado + ",\"items\":[{\"platoId\":"
				+ plato + ",\"cantidad\":" + cantidad + "}]}";
	}

	private String pedido(int mesa, int empleado, int platoA, int cantidadA, int platoB,
			int cantidadB) {
		return "{\"mesaId\":" + mesa + ",\"empleadoId\":" + empleado + ",\"items\":[{\"platoId\":"
				+ platoA + ",\"cantidad\":" + cantidadA + "},{\"platoId\":" + platoB
				+ ",\"cantidad\":" + cantidadB + "}]}";
	}

}

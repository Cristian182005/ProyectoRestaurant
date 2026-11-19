package com.elhornero.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elhornero.backend.entity.Merma;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class IngresoMermaInventarioTest extends BasePruebaIntegracion {

	private int proveedorId;
	private int empleadoId;

	@BeforeEach
	void prepararSocio() {
		proveedorId = crearProveedor().getId();
		empleadoId = crearEmpleado(9102, "Almacen").getId();
	}

	@Test
	void elIngresoDeInsumosActualizaElStock() throws Exception {
		int insumoA = crearInsumo("Arroz Test", 20.0, 5.0);
		int insumoB = crearInsumo("Azucar Test", 8.0, 3.0);

		MvcResult creacion = mockMvc
				.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON)
						.content("{\"proveedorId\":" + proveedorId + ",\"empleadoId\":" + empleadoId
								+ ",\"detalles\":[{\"insumoId\":" + insumoA
								+ ",\"cantidad\":10},{\"insumoId\":" + insumoB
								+ ",\"cantidad\":4}]}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.proveedorId").value(proveedorId))
				.andExpect(jsonPath("$.empleadoId").value(empleadoId))
				.andExpect(jsonPath("$.detalles.length()").value(2))
				.andExpect(jsonPath("$.detalles[0].insumoNombre").value("Arroz Test"))
				.andExpect(jsonPath("$.detalles[0].cantidad").value(10)).andReturn();
		int ingresoId = leerId(creacion);

		// el inventario quedo actualizado en PostgreSQL
		assertThat(insumoRepository.findById(insumoA)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(30.0));
		assertThat(insumoRepository.findById(insumoB)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(12.0));

		// READ
		mockMvc.perform(get("/api/ingresos/" + ingresoId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ingresoId))
				.andExpect(jsonPath("$.detalles.length()").value(2));
		mockMvc.perform(get("/api/ingresos/" + ingresoId + "/detalles"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.insumoNombre == 'Azucar Test')]").exists());
		mockMvc.perform(get("/api/ingresos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		assertThat(ingresoInsumoRepository.findByIdConDetalles(ingresoId).getDetalles())
				.hasSize(2);
	}

	@Test
	void rechazaIngresosInvalidos() throws Exception {
		int insumoId = crearInsumo("Papa Test", 5.0, 2.0);

		mockMvc.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"proveedorId\":" + proveedorId + ",\"empleadoId\":" + empleadoId
						+ ",\"detalles\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("El ingreso debe tener al menos un detalle"));

		mockMvc.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"proveedorId\":" + proveedorId + ",\"empleadoId\":" + empleadoId
						+ ",\"detalles\":[{\"insumoId\":" + insumoId + ",\"cantidad\":0}]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value(
						"La cantidad del insumo '" + insumoId + "' debe ser mayor a 0"));

		mockMvc.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"empleadoId\":" + empleadoId + ",\"detalles\":[{\"insumoId\":"
						+ insumoId + ",\"cantidad\":1}]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El proveedor del ingreso es obligatorio"));

		mockMvc.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"proveedorId\":999999,\"empleadoId\":" + empleadoId
						+ ",\"detalles\":[{\"insumoId\":" + insumoId + ",\"cantidad\":1}]}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Proveedor no encontrado con id 999999"));

		// nada quedo guardado
		assertThat(ingresoInsumoRepository.count()).isZero();
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(5.0));
	}

	@Test
	void laMermaDescuentaStockYSeRegistra() throws Exception {
		int insumoId = crearInsumo("Tomate Test", 15.0, 3.0);

		MvcResult creacion = mockMvc
				.perform(post("/api/mermas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"insumoId\":" + insumoId
								+ ",\"cantidad\":5,\"motivo\":\"Podrido\","
								+ "\"observacion\":\"Revisado\",\"empleadoId\":" + empleadoId + "}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.cantidad").value(5))
				.andExpect(jsonPath("$.motivo").value("Podrido"))
				.andExpect(jsonPath("$.estado").value("REGISTRADA"))
				.andExpect(jsonPath("$.insumo.id").value(insumoId)).andReturn();
		int mermaId = leerId(creacion);

		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(10.0));
		assertThat(mermaRepository.findById(mermaId)).isPresent();

		// READ
		mockMvc.perform(get("/api/mermas/" + mermaId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(mermaId));
		mockMvc.perform(get("/api/mermas")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/api/mermas/insumo/" + insumoId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/api/mermas/empleado/" + empleadoId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void laMermaConStockInsuficienteNoDejaNingunRegistro() throws Exception {
		int insumoId = crearInsumo("Leche Test", 4.0, 1.0);

		mockMvc.perform(post("/api/mermas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"insumoId\":" + insumoId
						+ ",\"cantidad\":10,\"motivo\":\"Caducada\",\"empleadoId\":" + empleadoId
						+ "}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(
						"Stock insuficiente de 'Leche Test': disponible 4.0, requerido 10.0"));

		// ni el stock cambio ni se creo la merma (transaccion revertida)
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(4.0));
		assertThat(mermaRepository.count()).isZero();
	}

	@Test
	void rechazaMermasInvalidas() throws Exception {
		int insumoId = crearInsumo("Queso Test", 6.0, 1.0);

		mockMvc.perform(post("/api/mermas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"insumoId\":" + insumoId
						+ ",\"cantidad\":0,\"motivo\":\"X\",\"empleadoId\":" + empleadoId + "}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La cantidad de la merma debe ser mayor a 0"));

		mockMvc.perform(post("/api/mermas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"insumoId\":" + insumoId + ",\"cantidad\":1,\"motivo\":\"  \","
						+ "\"empleadoId\":" + empleadoId + "}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El motivo de la merma es obligatorio"));

		mockMvc.perform(post("/api/mermas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"insumoId\":999999,\"cantidad\":1,\"motivo\":\"X\",\"empleadoId\":"
						+ empleadoId + "}"))
				.andExpect(status().isNotFound());

		assertThat(mermaRepository.count()).isZero();
		List<Merma> mermas = mermaRepository.findAllByOrderByFechaRegistroDesc();
		assertThat(mermas).isEmpty();
	}

	@Test
	void rechazaIngresosSinEmpleadoOConEmpleadoInexistente() throws Exception {
		int insumoId = crearInsumo("Lentejas Test", 6.0, 2.0);

		mockMvc.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"proveedorId\":" + proveedorId
						+ ",\"detalles\":[{\"insumoId\":" + insumoId + ",\"cantidad\":2}]}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El empleado que registra el ingreso es obligatorio"));

		mockMvc.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"proveedorId\":" + proveedorId + ",\"empleadoId\":999999,"
						+ "\"detalles\":[{\"insumoId\":" + insumoId + ",\"cantidad\":2}]}"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.mensaje")
						.value("Empleado no encontrado con id 999999"));

		assertThat(ingresoInsumoRepository.count()).isZero();
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(6.0));
	}

	@Test
	void unDetalleSinInsumoNoDejaRegistrosIncompletos() throws Exception {
		int insumoA = crearInsumo("Maiz Test", 12.0, 3.0);
		int insumoB = crearInsumo("Trigo Test", 9.0, 3.0);

		// el detalle valido se procesa primero pero nada debe quedar guardado
		mockMvc.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"proveedorId\":" + proveedorId + ",\"empleadoId\":" + empleadoId
						+ ",\"detalles\":[{\"insumoId\":" + insumoA + ",\"cantidad\":5},{"
						+ "\"cantidad\":2}]}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("Cada detalle del ingreso debe indicar el insumo"));

		// segundo insumo inexistente: tampoco puede quedar a medias
		mockMvc.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"proveedorId\":" + proveedorId + ",\"empleadoId\":" + empleadoId
						+ ",\"detalles\":[{\"insumoId\":" + insumoA + ",\"cantidad\":5},{"
						+ "\"insumoId\":999999,\"cantidad\":1}]}"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.mensaje")
						.value("Insumo no encontrado con id 999999"));

		assertThat(ingresoInsumoRepository.count()).isZero();
		assertThat(detalleIngresoRepository.count()).isZero();
		assertThat(insumoRepository.findById(insumoA)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(12.0));
		assertThat(insumoRepository.findById(insumoB)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(9.0));
	}

	@Test
	void aceptaFechaDeIngresoPersonalizada() throws Exception {
		int insumoId = crearInsumo("Sal Test", 4.0, 1.0);

		MvcResult creacion = mockMvc
				.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON)
						.content("{\"proveedorId\":" + proveedorId + ",\"empleadoId\":" + empleadoId
								+ ",\"fechaIngreso\":\"2026-05-20\",\"detalles\":[{\"insumoId\":"
								+ insumoId + ",\"cantidad\":6}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fechaIngreso").value("2026-05-20")).andReturn();
		int ingresoId = leerId(creacion);

		assertThat(ingresoInsumoRepository.findById(ingresoId)).hasValueSatisfying(
				ingreso -> assertThat(ingreso.getFechaIngreso()).hasToString("2026-05-20"));
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(10.0));
	}

	@Test
	void consultaIngresosInexistentes() throws Exception {
		mockMvc.perform(get("/api/ingresos/999999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Ingreso no encontrado con id 999999"));
		mockMvc.perform(get("/api/ingresos/999999/detalles")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Ingreso no encontrado con id 999999"));
	}

	@Test
	void rechazaMermasSinEmpleadoOConDatosInvalidos() throws Exception {
		int insumoId = crearInsumo("Mantequilla Test", 8.0, 2.0);

		mockMvc.perform(post("/api/mermas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"insumoId\":" + insumoId + ",\"cantidad\":1,\"motivo\":\"Vencida\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El empleado que registra la merma es obligatorio"));

		mockMvc.perform(post("/api/mermas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"insumoId\":" + insumoId + ",\"cantidad\":1,\"motivo\":\"Vencida\","
						+ "\"empleadoId\":999999}")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje")
						.value("Empleado no encontrado con id 999999"));

		mockMvc.perform(post("/api/mermas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"insumoId\":" + insumoId + ",\"cantidad\":null,"
						+ "\"motivo\":\"Vencida\",\"empleadoId\":" + empleadoId + "}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("La cantidad de la merma debe ser mayor a 0"));

		assertThat(mermaRepository.count()).isZero();
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(8.0));
	}

	@Test
	void consultaMermasPorIdentificadoresInexistentes() throws Exception {
		mockMvc.perform(get("/api/mermas/999999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Merma no encontrada con id 999999"));
		mockMvc.perform(get("/api/mermas/insumo/999999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Insumo no encontrado con id 999999"));
		mockMvc.perform(get("/api/mermas/empleado/999999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Empleado no encontrado con id 999999"));
	}

}

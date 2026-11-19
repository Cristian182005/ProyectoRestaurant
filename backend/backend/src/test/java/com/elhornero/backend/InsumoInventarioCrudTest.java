package com.elhornero.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class InsumoInventarioCrudTest extends BasePruebaIntegracion {

	@Test
	void crearConsultarActualizarYEliminarInsumo() throws Exception {
		// CREATE
		MvcResult creacion = mockMvc
				.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoInsumo("Harina Test", 25.0, 5.0, "KG")))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nombre").value("Harina Test"))
				.andExpect(jsonPath("$.stockActual").value(25.0))
				.andExpect(jsonPath("$.stockMinimo").value(5.0))
				.andExpect(jsonPath("$.unidadMedida").value("KG")).andReturn();
		int id = leerId(creacion);

		// persistencia en PostgreSQL
		assertThat(insumoRepository.findById(id)).hasValueSatisfying(insumo -> {
			assertThat(insumo.getNombre()).isEqualTo("Harina Test");
			assertThat(insumo.getStockActual()).isEqualTo(25.0);
		});

		// READ por id y listado
		mockMvc.perform(get("/api/insumos/" + id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id));
		mockMvc.perform(get("/api/insumos").param("buscar", "harina"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
		mockMvc.perform(get("/api/insumos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		// UPDATE
		mockMvc.perform(put("/api/insumos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoInsumo("Harina Test v2", 30.0, 8.0, "KG")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Harina Test v2"))
				.andExpect(jsonPath("$.stockActual").value(30.0));
		assertThat(insumoRepository.findById(id)).hasValueSatisfying(insumo -> {
			assertThat(insumo.getNombre()).isEqualTo("Harina Test v2");
			assertThat(insumo.getStockActual()).isEqualTo(30.0);
			assertThat(insumo.getStockMinimo()).isEqualTo(8.0);
		});

		// DELETE
		mockMvc.perform(delete("/api/insumos/" + id)).andExpect(status().isNoContent());
		assertThat(insumoRepository.findById(id)).isEmpty();
	}

	@Test
	void rechazaDatosInvalidosDelInsumo() throws Exception {
		mockMvc.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"\",\"unidadMedida\":\"KG\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El nombre del insumo es obligatorio"));

		mockMvc.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Sin unidad\",\"unidadMedida\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("La unidad de medida del insumo es obligatoria"));

		mockMvc.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Negativo\",\"unidadMedida\":\"KG\",\"stockActual\":-5}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El stock actual no puede ser negativo"));

		mockMvc.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Minimo negativo\",\"unidadMedida\":\"KG\",\"stockMinimo\":-1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El stock mínimo no puede ser negativo"));

		mockMvc.perform(get("/api/insumos/999999")).andExpect(status().isNotFound());
	}

	@Test
	void rechazaNombresDeInsumoDuplicados() throws Exception {
		crearInsumo("Sal Unica", 10.0, 2.0);

		mockMvc.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoInsumo("sal unica", 5.0, 1.0, "KG")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Ya existe un insumo con el nombre 'sal unica'"));

		assertThat(insumoRepository.count()).isEqualTo(1);
	}

	@Test
	void descuentaYAumentaStockSinPermitirStockNegativo() throws Exception {
		int id = crearInsumo("Aceite Test", 10.0, 2.0);

		// aumento
		mockMvc.perform(post("/api/insumos/" + id + "/aumentar-stock").param("cantidad", "5"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.stockActual").value(15.0));
		assertThat(insumoRepository.findById(id)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(15.0));

		// descuento valido
		mockMvc.perform(post("/api/insumos/" + id + "/descontar-stock").param("cantidad", "3"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.stockActual").value(12.0));

		// descuento mayor al disponible -> 409 y el stock NO cambia
		mockMvc.perform(post("/api/insumos/" + id + "/descontar-stock").param("cantidad", "50"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value(
						"Stock insuficiente de 'Aceite Test': disponible 12.0, requerido 50.0"));
		assertThat(insumoRepository.findById(id)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(12.0));

		// cantidades invalidas
		mockMvc.perform(post("/api/insumos/" + id + "/descontar-stock").param("cantidad", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La cantidad a descontar debe ser mayor a 0"));
		mockMvc.perform(post("/api/insumos/" + id + "/aumentar-stock").param("cantidad", "-2"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La cantidad a ingresar debe ser mayor a 0"));
		mockMvc.perform(post("/api/insumos/999999/descontar-stock").param("cantidad", "1"))
				.andExpect(status().isNotFound());
	}

	@Test
	void listadoDeInsumosConStockBajo() throws Exception {
		crearInsumo("Insumo Bajo", 2.0, 5.0);
		crearInsumo("Insumo Sano", 50.0, 5.0);

		mockMvc.perform(get("/api/insumos/stock-bajo")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nombre").value("Insumo Bajo"));
	}

	@Test
	void noPermiteEliminarInsumoRelacionado() throws Exception {
		int insumoId = crearInsumo("Insumo Relacionado", 10.0, 1.0);
		int platoId = crearPlato("Plato con insumo", "Platos", 20.0, true);

		mockMvc.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoId + ",\"insumoId\":" + insumoId
						+ ",\"cantidadRequerida\":1.5}"))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/insumos/" + insumoId)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value(
						"No se puede eliminar el insumo porque es ingrediente de platos"));

		// quitar la relacion permite eliminarlo
		int ingredienteId = leerIdDeLista(mockMvc
				.perform(get("/api/ingredientes/plato/" + platoId)).andExpect(status().isOk())
				.andReturn());
		mockMvc.perform(delete("/api/ingredientes/" + ingredienteId))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/insumos/" + insumoId)).andExpect(status().isNoContent());
		assertThat(insumoRepository.findById(insumoId)).isEmpty();
	}

	@Test
	void actualizarInsumoRechazaNombresDuplicados() throws Exception {
		crearInsumo("Azucar Unica", 8.0, 2.0);
		int otroId = crearInsumo("Sal Duplicada", 5.0, 1.0);

		mockMvc.perform(put("/api/insumos/" + otroId).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoInsumo("azucar unica", 5.0, 1.0, "KG")))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje")
						.value("Ya existe otro insumo con el nombre 'azucar unica'"));

		assertThat(insumoRepository.findById(otroId)).hasValueSatisfying(insumo -> {
			assertThat(insumo.getNombre()).isEqualTo("Sal Duplicada");
			assertThat(insumo.getStockActual()).isEqualTo(5.0);
		});
		assertThat(insumoRepository.count()).isEqualTo(2);
	}

	@Test
	void actualizarInsumoRechazaDatosInvalidosSinTocarLaBase() throws Exception {
		int id = crearInsumo("Pimienta Test", 7.0, 2.0);

		mockMvc.perform(put("/api/insumos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoInsumo("Pimienta Test", -3.0, 2.0, "KG")))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El stock actual no puede ser negativo"));
		mockMvc.perform(put("/api/insumos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoInsumo("Pimienta Test", 7.0, 2.0, "")))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("La unidad de medida del insumo es obligatoria"));
		mockMvc.perform(put("/api/insumos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"\",\"unidadMedida\":\"KG\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El nombre del insumo es obligatorio"));

		assertThat(insumoRepository.findById(id)).hasValueSatisfying(insumo -> {
			assertThat(insumo.getNombre()).isEqualTo("Pimienta Test");
			assertThat(insumo.getStockActual()).isEqualTo(7.0);
			assertThat(insumo.getUnidadMedida()).isEqualTo("KG");
		});
	}

	@Test
	void noPermiteEliminarInsumoConIngresosRegistrados() throws Exception {
		int proveedorId = crearProveedor().getId();
		int empleadoId = crearEmpleado(9452, "Almacen").getId();
		int insumoId = crearInsumo("Insumo Con Ingreso", 10.0, 1.0);

		mockMvc.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"proveedorId\":" + proveedorId + ",\"empleadoId\":" + empleadoId
						+ ",\"detalles\":[{\"insumoId\":" + insumoId + ",\"cantidad\":5}]}"))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/insumos/" + insumoId)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value(
						"No se puede eliminar el insumo porque tiene ingresos registrados"));

		assertThat(insumoRepository.findById(insumoId)).isPresent();
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(15.0));
	}

	@Test
	void noPermiteEliminarInsumoConMermasRegistradas() throws Exception {
		int empleadoId = crearEmpleado(9453, "Almacen").getId();
		int insumoId = crearInsumo("Insumo Con Merma", 10.0, 1.0);

		mockMvc.perform(post("/api/mermas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"insumoId\":" + insumoId + ",\"cantidad\":2,"
						+ "\"motivo\":\"Dañado\",\"empleadoId\":" + empleadoId + "}"))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/insumos/" + insumoId)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje")
						.value("No se puede eliminar el insumo porque tiene mermas registradas"));

		assertThat(insumoRepository.findById(insumoId)).isPresent();
		assertThat(insumoRepository.findById(insumoId)).hasValueSatisfying(
				insumo -> assertThat(insumo.getStockActual()).isEqualTo(8.0));
	}

	@Test
	void consultasDeStockConIdentificadoresInvalidos() throws Exception {
		mockMvc.perform(post("/api/insumos/999999/aumentar-stock").param("cantidad", "1"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Insumo no encontrado con id 999999"));
		mockMvc.perform(post("/api/insumos/999999/descontar-stock").param("cantidad", "1"))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/insumos/1/aumentar-stock").param("cantidad", "abc"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El parametro 'cantidad' tiene un valor invalido"));

		assertThat(insumoRepository.count()).isZero();
	}

}

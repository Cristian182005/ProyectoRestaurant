package com.elhornero.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Cubre el CRUD completo de la relacion plato-insumo (ingredientes): registro,
 * consultas por plato e insumo, actualizacion de cantidad, baja, duplicados y
 * referencias inexistentes, verificando siempre la persistencia en PostgreSQL.
 */
class IngredientePlatoRelacionTest extends BasePruebaIntegracion {

	private int platoId;
	private int insumoId;

	@BeforeEach
	void preparar() throws Exception {
		platoId = crearPlato("Plato Ingrediente", "Platos", 28.0, true);
		insumoId = crearInsumo("Arroz Ingrediente", 12.0, 2.0);
	}

	@Test
	void crudDeLaRelacionPlatoEInsumo() throws Exception {
		// CREATE
		MvcResult creacion = mockMvc
				.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
						.content("{\"platoId\":" + platoId + ",\"insumoId\":" + insumoId
								+ ",\"cantidadRequerida\":1.5}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.cantidadRequerida").value(1.5)).andReturn();
		int ingredienteId = leerId(creacion);
		assertThat(ingredientePlatoRepository.count()).isEqualTo(1);

		// READ por plato, por insumo y por id
		mockMvc.perform(get("/api/ingredientes/plato/" + platoId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].insumo.id").value(insumoId));
		mockMvc.perform(get("/api/ingredientes/insumo/" + insumoId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].plato.id").value(platoId));
		mockMvc.perform(get("/api/ingredientes/" + ingredienteId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ingredienteId))
				.andExpect(jsonPath("$.plato.id").value(platoId))
				.andExpect(jsonPath("$.insumo.id").value(insumoId));

		// UPDATE de la cantidad requerida
		mockMvc.perform(put("/api/ingredientes/" + ingredienteId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"cantidadRequerida\":3}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.cantidadRequerida").value(3));
		assertThat(ingredientePlatoRepository.findById(ingredienteId)).hasValueSatisfying(
				ingrediente -> assertThat(ingrediente.getCantidadRequerida()).isEqualTo(3.0));

		// DELETE
		mockMvc.perform(delete("/api/ingredientes/" + ingredienteId))
				.andExpect(status().isNoContent());
		assertThat(ingredientePlatoRepository.findById(ingredienteId)).isEmpty();
	}

	@Test
	void rechazaCantidadesInvalidasDelIngrediente() throws Exception {
		MvcResult creacion = mockMvc
				.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
						.content("{\"platoId\":" + platoId + ",\"insumoId\":" + insumoId
								+ ",\"cantidadRequerida\":2}"))
				.andExpect(status().isCreated()).andReturn();
		int ingredienteId = leerId(creacion);

		mockMvc.perform(put("/api/ingredientes/" + ingredienteId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"cantidadRequerida\":0}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("La cantidad requerida del insumo debe ser mayor a 0"));
		mockMvc.perform(put("/api/ingredientes/" + ingredienteId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"cantidadRequerida\":null}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("La cantidad requerida del insumo es obligatoria"));
		mockMvc.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoId + ",\"insumoId\":" + insumoId + "}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("La cantidad requerida del insumo es obligatoria"));

		// nada cambio en la base de datos
		assertThat(ingredientePlatoRepository.findById(ingredienteId)).hasValueSatisfying(
				ingrediente -> assertThat(ingrediente.getCantidadRequerida()).isEqualTo(2.0));
		assertThat(ingredientePlatoRepository.count()).isEqualTo(1);
	}

	@Test
	void rechazaRelacionesDuplicadas() throws Exception {
		mockMvc.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoId + ",\"insumoId\":" + insumoId
						+ ",\"cantidadRequerida\":1}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoId + ",\"insumoId\":" + insumoId
						+ ",\"cantidadRequerida\":4}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(
						"El plato 'Plato Ingrediente' ya tiene registrado el insumo 'Arroz Ingrediente'"));

		assertThat(ingredientePlatoRepository.count()).isEqualTo(1);
	}

	@Test
	void consultaYBajaDeIngredientesInexistentes() throws Exception {
		mockMvc.perform(get("/api/ingredientes/plato/999999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Plato no encontrado con id 999999"));
		mockMvc.perform(get("/api/ingredientes/insumo/999999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Insumo no encontrado con id 999999"));
		mockMvc.perform(get("/api/ingredientes/999999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Ingrediente no encontrado con id 999999"));
		mockMvc.perform(put("/api/ingredientes/999999").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cantidadRequerida\":2}")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Ingrediente no encontrado con id 999999"));
		mockMvc.perform(delete("/api/ingredientes/999999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Ingrediente no encontrado con id 999999"));

		assertThat(ingredientePlatoRepository.count()).isZero();
	}

	@Test
	void rechazaReferenciasInexistentesAlCrearElIngrediente() throws Exception {
		mockMvc.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":999999,\"insumoId\":" + insumoId
						+ ",\"cantidadRequerida\":1}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Plato no encontrado con id 999999"));
		mockMvc.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoId + ",\"insumoId\":999999,"
						+ "\"cantidadRequerida\":1}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Insumo no encontrado con id 999999"));
		mockMvc.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"insumoId\":" + insumoId + ",\"cantidadRequerida\":1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El id del plato es obligatorio"));
		mockMvc.perform(post("/api/ingredientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"platoId\":" + platoId + ",\"cantidadRequerida\":1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El id del insumo es obligatorio"));

		assertThat(ingredientePlatoRepository.count()).isZero();
	}

}

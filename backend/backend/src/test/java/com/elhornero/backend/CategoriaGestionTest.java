package com.elhornero.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class CategoriaGestionTest extends BasePruebaIntegracion {

	@Test
	void listaYConsultaCategoriasDerivadasDeLosPlatos() throws Exception {
		crearPlato("Plato Bebida A", "Bebidas", 10.0, true);
		crearPlato("Plato Postre", "Postres", 12.0, true);
		crearPlato("Plato Bebida B", "Bebidas", 8.0, true);

		mockMvc.perform(get("/api/categorias")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0]").value("Bebidas"))
				.andExpect(jsonPath("$[1]").value("Postres"));

		mockMvc.perform(get("/api/categorias/Bebidas")).andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Bebidas"))
				.andExpect(jsonPath("$.productos").value(2));

		mockMvc.perform(get("/api/categorias/Bebidas/productos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.categoria == 'Bebidas')]").exists());

		mockMvc.perform(get("/api/categorias/Inexistente")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Categoria no encontrada con nombre Inexistente"));
		mockMvc.perform(get("/api/categorias/Inexistente/productos"))
				.andExpect(status().isNotFound());
	}

	@Test
	void renombraLaCategoriaEnTodosSusPlatos() throws Exception {
		crearPlato("Plato Uno", "Bebidas", 10.0, true);
		crearPlato("Plato Dos", "Bebidas", 15.0, false);
		crearPlato("Plato Tres", "Postres", 20.0, true);

		MvcResult renombre = mockMvc
				.perform(put("/api/categorias/Bebidas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Jugos\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Jugos"))
				.andExpect(jsonPath("$.productos").value(2)).andReturn();
		assertThat(renombre.getResponse().getContentAsString()).contains("Jugos");

		// los tres platos quedaron actualizados en PostgreSQL
		List<com.elhornero.backend.entity.Plato> actualizados = platoRepository
				.findByCategoriaIgnoreCase("Jugos");
		assertThat(actualizados).hasSize(2);
		assertThat(platoRepository.findByCategoriaIgnoreCase("Bebidas")).isEmpty();
		assertThat(platoRepository.listarCategoriasDistinct()).containsExactly("Jugos",
				"Postres");

		mockMvc.perform(get("/api/categorias/Bebidas")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/categorias/Jugos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.productos").value(2));
		mockMvc.perform(get("/api/platos").param("categoria", "Jugos"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void rechazaRenombresInvalidos() throws Exception {
		crearPlato("Plato Bebidas", "Bebidas", 10.0, true);
		crearPlato("Plato Refresco", "Refrescos", 10.0, true);

		// categoria inexistente
		mockMvc.perform(put("/api/categorias/Inexistente").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Nueva\"}")).andExpect(status().isNotFound());

		// destino ocupado por otra categoria
		mockMvc.perform(put("/api/categorias/Bebidas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Refrescos\"}")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Ya existe la categoria 'Refrescos'"));

		// nombre vacio
		mockMvc.perform(put("/api/categorias/Bebidas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"   \"}")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El nombre de la categoria es obligatorio"));

		// cuerpo nulo
		mockMvc.perform(put("/api/categorias/Bebidas").contentType(MediaType.APPLICATION_JSON)
				.content("null")).andExpect(status().isBadRequest());

		// mismo nombre
		mockMvc.perform(put("/api/categorias/Bebidas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"bebidas\"}")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("El nuevo nombre de la categoria debe ser diferente al actual"));

		// nada cambio en la base de datos
		assertThat(platoRepository.listarCategoriasDistinct()).containsExactly("Bebidas",
				"Refrescos");
	}

	@Test
	void noPermiteEliminarCategoriaConPlatosRelacionados() throws Exception {
		crearPlato("Plato con categoria", "Bebidas", 10.0, true);

		mockMvc.perform(delete("/api/categorias/Bebidas")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value(
						"No se puede eliminar la categoria porque tiene 1 producto(s) relacionado(s)"));

		// el plato y la categoria siguen intactos
		assertThat(platoRepository.findByCategoriaIgnoreCase("Bebidas")).hasSize(1);
		assertThat(platoRepository.listarCategoriasDistinct()).containsExactly("Bebidas");

		mockMvc.perform(delete("/api/categorias/Inexistente")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Categoria no encontrada con nombre Inexistente"));
	}

	@Test
	void cambiaLaDisponibilidadDelPlato() throws Exception {
		int id = crearPlato("Plato Disponible", "Platos", 30.0, true);

		mockMvc.perform(put("/api/platos/" + id + "/disponibilidad")
				.contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.activo").value(false));
		assertThat(platoRepository.findById(id)).hasValueSatisfying(
				plato -> assertThat(plato.getActivo()).isFalse());

		mockMvc.perform(get("/api/platos").param("activo", "true"))
				.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		mockMvc.perform(get("/api/platos").param("activo", "false")).andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());

		// volver a habilitar
		mockMvc.perform(put("/api/platos/" + id + "/disponibilidad")
				.contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
		assertThat(platoRepository.findById(id)).hasValueSatisfying(
				plato -> assertThat(plato.getActivo()).isTrue());

		// validaciones
		mockMvc.perform(put("/api/platos/" + id + "/disponibilidad")
				.contentType(MediaType.APPLICATION_JSON).content("{\"activo\":null}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("La disponibilidad del plato es obligatoria"));
		mockMvc.perform(put("/api/platos/999999/disponibilidad")
				.contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void rechazaLongitudesQueNoEntranEnLasColumnas() throws Exception {
		String nombreLargo = "x".repeat(101);
		mockMvc.perform(post("/api/platos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"" + nombreLargo + "\",\"categoria\":\"Platos\",\"precio\":10}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El nombre del plato no puede superar 100 caracteres"));

		String categoriaLarga = "y".repeat(51);
		mockMvc.perform(post("/api/platos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Plato Validado\",\"categoria\":\"" + categoriaLarga
						+ "\",\"precio\":10}")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("La categoría del plato no puede superar 50 caracteres"));

		mockMvc.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"" + nombreLargo + "\",\"unidadMedida\":\"KG\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El nombre del insumo no puede superar 100 caracteres"));

		assertThat(platoRepository.count()).isZero();
		assertThat(insumoRepository.count()).isZero();
	}

}

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

class PlatoCategoriaCrudTest extends BasePruebaIntegracion {

	@Test
	void crearConsultarActualizarYEliminarPlato() throws Exception {
		// CREATE
		MvcResult creacion = mockMvc
				.perform(post("/api/platos").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoPlato("Pizza Test", "Pizzas", 45.5, true)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nombre").value("Pizza Test"))
				.andExpect(jsonPath("$.categoria").value("Pizzas"))
				.andExpect(jsonPath("$.precio").value(45.5))
				.andExpect(jsonPath("$.activo").value(true)).andReturn();
		int id = leerId(creacion);

		// persistencia real en PostgreSQL
		assertThat(platoRepository.findById(id)).hasValueSatisfying(plato -> {
			assertThat(plato.getNombre()).isEqualTo("Pizza Test");
			assertThat(plato.getPrecio()).isEqualTo(45.5);
		});

		// READ por id
		mockMvc.perform(get("/api/platos/" + id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.descripcion").value("Plato de prueba"));

		// READ listado con filtros
		mockMvc.perform(get("/api/platos").param("categoria", "Pizzas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
		mockMvc.perform(get("/api/platos").param("buscar", "pizz"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
		mockMvc.perform(get("/api/platos").param("activo", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());

		// UPDATE
		mockMvc.perform(put("/api/platos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoPlato("Pizza Test Alterada", "Pizzas", 55.0, true)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Pizza Test Alterada"))
				.andExpect(jsonPath("$.precio").value(55.0));
		assertThat(platoRepository.findById(id)).hasValueSatisfying(plato -> {
			assertThat(plato.getNombre()).isEqualTo("Pizza Test Alterada");
			assertThat(plato.getPrecio()).isEqualTo(55.0);
		});

		// DELETE: el primero deja el plato inactivo (baja logica) y el segundo lo elimina
		mockMvc.perform(delete("/api/platos/" + id)).andExpect(status().isNoContent());
		assertThat(platoRepository.findById(id)).hasValueSatisfying(
				plato -> assertThat(plato.getActivo()).isFalse());

		mockMvc.perform(delete("/api/platos/" + id)).andExpect(status().isNoContent());
		assertThat(platoRepository.findById(id)).isEmpty();
	}

	@Test
	void rechazaDatosInvalidosDelPlato() throws Exception {
		mockMvc.perform(post("/api/platos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"\",\"categoria\":\"Pizzas\",\"precio\":10}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El nombre del plato es obligatorio"));

		mockMvc.perform(post("/api/platos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Sin categoria\",\"categoria\":\"\",\"precio\":10}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La categoría del plato es obligatoria"));

		mockMvc.perform(post("/api/platos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Sin precio\",\"categoria\":\"Pizzas\",\"precio\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El precio del plato debe ser mayor a 0"));

		mockMvc.perform(post("/api/platos").contentType(MediaType.APPLICATION_JSON).content("{"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/platos/999999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("Plato no encontrado con id 999999"));
		mockMvc.perform(put("/api/platos/999999").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoPlato("Otro", "Pizzas", 10, true)))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/platos/999999")).andExpect(status().isNotFound());
	}

	@Test
	void rechazaNombresDePlatoDuplicados() throws Exception {
		crearPlato("Ceviche Unico", "Entradas", 30.0, true);

		mockMvc.perform(post("/api/platos").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoPlato("ceviche unico", "Entradas", 25.0, true)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value(
						"Ya existe un plato con el nombre 'ceviche unico'"));

		assertThat(platoRepository.count()).isEqualTo(1);
	}

	@Test
	void listaLasCategoriasRegistradas() throws Exception {
		crearPlato("Plato A", "Bebidas", 10.0, true);
		crearPlato("Plato B", "Postres", 12.0, true);
		crearPlato("Plato C", "Bebidas", 8.0, true);

		mockMvc.perform(get("/api/categorias")).andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(2));

		mockMvc.perform(get("/api/platos/categorias")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		List<String> categorias = platoRepository.listarCategoriasDistinct();
		assertThat(categorias).containsExactly("Bebidas", "Postres");
	}

	@Test
	void actualizarPlatoRechazaNombresDuplicados() throws Exception {
		crearPlato("Plato Original", "Platos", 20.0, true);
		int otroId = crearPlato("Plato Duplicado", "Platos", 15.0, true);

		// el nombre ya pertenece a otro plato
		mockMvc.perform(put("/api/platos/" + otroId).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoPlato("plato original", "Platos", 15.0, true)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje")
						.value("Ya existe otro plato con el nombre 'plato original'"));

		// conservar el propio nombre si sigue disponible
		mockMvc.perform(put("/api/platos/" + otroId).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoPlato("Plato Duplicado", "Platos", 16.0, true)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.precio").value(16.0));

		assertThat(platoRepository.findById(otroId)).hasValueSatisfying(plato -> {
			assertThat(plato.getNombre()).isEqualTo("Plato Duplicado");
			assertThat(plato.getPrecio()).isEqualTo(16.0);
		});
		assertThat(platoRepository.count()).isEqualTo(2);
	}

	@Test
	void actualizarPlatoRechazaDatosInvalidosSinTocarLaBase() throws Exception {
		int id = crearPlato("Plato Actualizable", "Platos", 22.0, true);

		mockMvc.perform(put("/api/platos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"\",\"categoria\":\"Platos\",\"precio\":10}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El nombre del plato es obligatorio"));
		mockMvc.perform(put("/api/platos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoPlato("Plato Actualizable", "Platos", 0, true)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("El precio del plato debe ser mayor a 0"));
		mockMvc.perform(put("/api/platos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoPlato("Plato Actualizable", "Platos", 10, true).replace(
						"plato.png", "x".repeat(256))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("La imagen del plato no puede superar 255 caracteres"));
		mockMvc.perform(put("/api/platos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Plato Actualizable\",\"categoria\":\"\",\"precio\":10}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("La categoría del plato es obligatoria"));

		// ningun cambio llego a PostgreSQL
		assertThat(platoRepository.findById(id)).hasValueSatisfying(plato -> {
			assertThat(plato.getNombre()).isEqualTo("Plato Actualizable");
			assertThat(plato.getPrecio()).isEqualTo(22.0);
			assertThat(plato.getCategoria()).isEqualTo("Platos");
		});
	}

	@Test
	void noPermiteEliminarPlatoQueTienePedidos() throws Exception {
		int platoId = crearPlato("Plato con pedido", "Platos", 20.0, true);
		int mesaId = crearMesa(90);
		crearEmpleado(9010, "Mesero");

		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"mesaId\":" + mesaId + ",\"empleadoId\":9010,\"items\":[{\"platoId\":"
						+ platoId + ",\"cantidad\":1}]}"))
				.andExpect(status().isCreated());

		// baja logica primero
		mockMvc.perform(delete("/api/platos/" + platoId)).andExpect(status().isNoContent());
		assertThat(platoRepository.findById(platoId)).hasValueSatisfying(
				plato -> assertThat(plato.getActivo()).isFalse());

		// intento de baja definitiva: hay pedidos asociados
		mockMvc.perform(delete("/api/platos/" + platoId)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value(
						"No se puede eliminar el plato porque tiene pedidos asociados"));

		assertThat(platoRepository.findById(platoId)).isPresent();
		assertThat(pedidoRepository.findAll()).hasSize(1);
	}

}

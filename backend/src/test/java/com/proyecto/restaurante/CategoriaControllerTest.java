package com.proyecto.restaurante;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void crearListarActualizarYEliminarCategoria() throws Exception {
        String cuerpo = """
                {"nombre": "Para Llevar", "descripcion": "Pedidos de salida"}
                """;

        MvcResult creada = mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Para Llevar"))
                .andReturn();

        Long id = ((Number) JsonPath.read(creada.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/categorias/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Para Llevar"));

        mockMvc.perform(put("/api/categorias/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Combos\", \"descripcion\": \"Ofertas del día\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Combos"));

        mockMvc.perform(delete("/api/categorias/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categorias/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void categoriaDuplicadaResponde409() throws Exception {
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Bebidas\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Bebidas")));
    }

    @Test
    void categoriaSinNombreResponde400() throws Exception {
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
    }

    @Test
    void categoriaConProductosNoSePuedeEliminar() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/categorias?buscar=Bebidas"))
                .andExpect(status().isOk())
                .andReturn();
        List<Object> ids = JsonPath.read(resultado.getResponse().getContentAsString(),
                "$[?(@.nombre == 'Bebidas')].id");
        Long idBebidas = ((Number) ids.get(0)).longValue();

        mockMvc.perform(delete("/api/categorias/" + idBebidas))
                .andExpect(status().isConflict());
    }
}

package com.proyecto.restaurante;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private Long crearProducto(String nombre, double precio, int stock, Long categoriaId) throws Exception {
        String cuerpo = """
                {"nombre": "%s", "descripcion": "De prueba", "precio": %s, "stock": %d, "categoriaId": %d}
                """.formatted(nombre, precio, stock, categoriaId);
        MvcResult resultado = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private Long categoriaDeEjemplo() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/categorias?buscar=Entradas"))
                .andExpect(status().isOk())
                .andReturn();
        java.util.List<Object> ids = JsonPath.read(resultado.getResponse().getContentAsString(),
                "$[?(@.nombre == 'Entradas')].id");
        return ((Number) ids.get(0)).longValue();
    }

    @Test
    void crearObtenerYActualizarProducto() throws Exception {
        Long categoriaId = categoriaDeEjemplo();
        Long id = crearProducto("Anticucho de prueba", 15.50, 10, categoriaId);

        mockMvc.perform(get("/api/productos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Anticucho de prueba"))
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.categoriaNombre").value("Entradas"));

        String cuerpo = """
                {"nombre": "Anticucho XL", "descripcion": "Actualizado", "precio": 18.00,
                 "stock": 12, "categoriaId": %d}
                """.formatted(categoriaId);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/productos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Anticucho XL"))
                .andExpect(jsonPath("$.precio").value(18.00));
    }

    @Test
    void filtrarPorCategoriaYTexto() throws Exception {
        Long categoriaId = categoriaDeEjemplo();
        crearProducto("Sopa del día especial", 10.00, 5, categoriaId);

        mockMvc.perform(get("/api/productos?categoriaId=" + categoriaId + "&buscar=Sopa del día"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Sopa del día especial')]").exists());
    }

    @Test
    void actualizarStockRespondeConNuevoValor() throws Exception {
        Long categoriaId = categoriaDeEjemplo();
        Long id = crearProducto("Producto stock", 8.00, 5, categoriaId);

        mockMvc.perform(patch("/api/productos/" + id + "/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(25));

        mockMvc.perform(patch("/api/productos/" + id + "/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": -3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarProductoLoDesactiva() throws Exception {
        Long categoriaId = categoriaDeEjemplo();
        Long id = crearProducto("Producto a dar de baja", 6.00, 4, categoriaId);

        mockMvc.perform(delete("/api/productos/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/productos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(get("/api/productos?buscar=Producto a dar de baja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void productoSinCategoriaResponde400() throws Exception {
        String cuerpo = """
                {"nombre": "Sin categoría", "precio": 10, "stock": 3}
                """;
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
    }
}

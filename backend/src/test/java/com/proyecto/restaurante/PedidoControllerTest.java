package com.proyecto.restaurante;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private Long categoriaId() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/categorias?buscar=Entradas"))
                .andExpect(status().isOk())
                .andReturn();
        java.util.List<Object> ids = JsonPath.read(resultado.getResponse().getContentAsString(),
                "$[?(@.nombre == 'Entradas')].id");
        return ((Number) ids.get(0)).longValue();
    }

    private Long crearProducto(String nombre, double precio, int stock) throws Exception {
        String cuerpo = """
                {"nombre": "%s", "precio": %s, "stock": %d, "categoriaId": %d}
                """.formatted(nombre, precio, stock, categoriaId());
        MvcResult resultado = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void crearPedidoDescuentaStockYCalculaTotal() throws Exception {
        Long productoId = crearProducto("Pedido Test - Filete", 20.00, 10);

        String cuerpo = """
                {"metodoPago": "EFECTIVO", "cliente": "Ana", "mesa": "Mesa 3",
                 "items": [{"productoId": %d, "cantidad": 3}]}
                """.formatted(productoId);
        MvcResult resultado = mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.total").value(60.00))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn();
        Long pedidoId = ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/productos/" + productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(7));

        mockMvc.perform(get("/api/pedidos/" + pedidoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productoNombre").value("Pedido Test - Filete"))
                .andExpect(jsonPath("$.items[0].subtotal").value(60.00));
    }

    @Test
    void pedidoConStockInsuficienteResponde409YNoModificaStock() throws Exception {
        Long productoId = crearProducto("Pedido Test - Agotado", 15.00, 2);

        String cuerpo = """
                {"metodoPago": "YAPE",
                 "items": [{"productoId": %d, "cantidad": 5}]}
                """.formatted(productoId);
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString("Stock insuficiente")));

        mockMvc.perform(get("/api/productos/" + productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(2));
    }

    @Test
    void cancelarPedidoDevuelveElStock() throws Exception {
        Long productoId = crearProducto("Pedido Test - Cancelación", 10.00, 8);

        String cuerpo = """
                {"metodoPago": "TARJETA",
                 "items": [{"productoId": %d, "cantidad": 4}]}
                """.formatted(productoId);
        MvcResult resultado = mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
        Long pedidoId = ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/productos/" + productoId))
                .andExpect(jsonPath("$.stock").value(4));

        mockMvc.perform(patch("/api/pedidos/" + pedidoId + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"CANCELADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        mockMvc.perform(get("/api/productos/" + productoId))
                .andExpect(jsonPath("$.stock").value(8));

        mockMvc.perform(patch("/api/pedidos/" + pedidoId + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"ENTREGADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void pedidosVaciosDebenSerUnaLista() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void pedidoSinItemsResponde400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\": \"EFECTIVO\", \"items\": []}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pedidoConProductoInexistenteResponde404() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metodoPago": "EFECTIVO",
                                 "items": [{"productoId": 999999, "cantidad": 1}]}
                                """))
                .andExpect(status().isNotFound());
    }
}

package com.proyecto.restaurante;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void resumenDeVentasIncluyePedidosNoCancelados() throws Exception {
        MvcResult categorias = mockMvc.perform(get("/api/categorias?buscar=Bebidas"))
                .andExpect(status().isOk())
                .andReturn();
        java.util.List<Object> ids = JsonPath.read(categorias.getResponse().getContentAsString(),
                "$[?(@.nombre == 'Bebidas')].id");
        Long categoriaId = ((Number) ids.get(0)).longValue();

        MvcResult producto = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Venta Test - Jugo", "precio": 7.50, "stock": 20,
                                 "categoriaId": %d}
                                """.formatted(categoriaId)))
                .andExpect(status().isCreated())
                .andReturn();
        Long productoId = ((Number) JsonPath.read(producto.getResponse().getContentAsString(), "$.id"))
                .longValue();

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metodoPago": "PLIN", "cliente": "Luis",
                                 "items": [{"productoId": %d, "cantidad": 4}]}
                                """.formatted(productoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(30.00));

        String desde = LocalDate.now().minusDays(1).toString();
        String hasta = LocalDate.now().toString();

        mockMvc.perform(get("/api/ventas/resumen?desde=" + desde + "&hasta=" + hasta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value(desde))
                .andExpect(jsonPath("$.hasta").value(hasta))
                .andExpect(jsonPath("$.cantidadPedidos").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThan(0.0)))
                .andExpect(jsonPath("$.productosMasVendidos").isArray())
                .andExpect(jsonPath("$.productosMasVendidos.length()").value(
                        org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void resumenSinPedidosDevuelveCeros() throws Exception {
        String desde = LocalDate.now().plusDays(10).toString();
        String hasta = LocalDate.now().plusDays(20).toString();

        mockMvc.perform(get("/api/ventas/resumen?desde=" + desde + "&hasta=" + hasta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadPedidos").value(0))
                .andExpect(jsonPath("$.total").value(0.0))
                .andExpect(jsonPath("$.productosMasVendidos.length()").value(0));
    }
}

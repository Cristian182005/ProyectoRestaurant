package com.proyecto.restaurante.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record VentasResumenResponse(
        LocalDate desde,
        LocalDate hasta,
        int cantidadPedidos,
        BigDecimal total,
        BigDecimal promedio,
        List<ProductoVendidoResponse> productosMasVendidos) {
}

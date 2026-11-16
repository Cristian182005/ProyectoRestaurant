package com.proyecto.restaurante.dto;

import java.math.BigDecimal;

public record ProductoVendidoResponse(
        Long productoId,
        String producto,
        int cantidadVendida,
        BigDecimal totalVendido) {
}

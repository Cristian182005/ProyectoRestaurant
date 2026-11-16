package com.proyecto.restaurante.dto;

import com.proyecto.restaurante.entity.Producto;
import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        Boolean activo,
        Long categoriaId,
        String categoriaNombre) {

    public static ProductoResponse from(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getActivo(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre());
    }
}

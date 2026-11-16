package com.proyecto.restaurante.dto;

import com.proyecto.restaurante.entity.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record EstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPedido estado) {
}

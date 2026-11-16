package com.proyecto.restaurante.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.proyecto.restaurante.entity.MetodoPago;
import java.util.List;

public record PedidoRequest(
        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        String cliente,

        String mesa,

        @NotEmpty(message = "El pedido debe tener al menos un producto")
        List<@Valid ItemPedidoRequest> items) {
}

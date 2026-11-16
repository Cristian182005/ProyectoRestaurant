package com.proyecto.restaurante.dto;

import com.proyecto.restaurante.entity.EstadoPedido;
import com.proyecto.restaurante.entity.MetodoPago;
import com.proyecto.restaurante.entity.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        LocalDateTime fecha,
        EstadoPedido estado,
        BigDecimal total,
        MetodoPago metodoPago,
        String cliente,
        String mesa,
        List<DetallePedidoResponse> items) {

    public static PedidoResponse from(Pedido pedido) {
        List<DetallePedidoResponse> items = pedido.getDetalles().stream()
                .map(DetallePedidoResponse::from)
                .toList();
        return new PedidoResponse(
                pedido.getId(),
                pedido.getFecha(),
                pedido.getEstado(),
                pedido.getTotal(),
                pedido.getMetodoPago(),
                pedido.getCliente(),
                pedido.getMesa(),
                items);
    }
}

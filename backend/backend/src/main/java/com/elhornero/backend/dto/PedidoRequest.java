package com.elhornero.backend.dto;

import java.util.List;

public record PedidoRequest(Integer mesaId, Integer empleadoId, List<ItemPedidoRequest> items) {
}

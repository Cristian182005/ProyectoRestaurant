package com.elhornero.backend.dto;

import com.elhornero.backend.entity.TipoPago;

public record ComprobanteRequest(Integer pedidoId, Integer cajeroId, TipoPago tipoPago,
		Double montoRecibido, Integer clienteNaturalId, Integer clienteJuridicoId) {
}

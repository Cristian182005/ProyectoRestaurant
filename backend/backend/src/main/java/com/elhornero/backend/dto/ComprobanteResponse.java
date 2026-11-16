package com.elhornero.backend.dto;

import com.elhornero.backend.entity.Comprobante;
import com.elhornero.backend.entity.TipoPago;
import java.time.LocalDate;
import java.time.LocalTime;

public record ComprobanteResponse(Integer id, LocalDate fechaEmision, LocalTime horaEmision,
		Double gravado, Double igv, Double precioTotal, TipoPago tipoPago, Double vueltoPago,
		Double montoRecibido, Integer cajeroId, Integer pedidoId) {

	public static ComprobanteResponse from(Comprobante comprobante) {
		return new ComprobanteResponse(comprobante.getId(), comprobante.getFechaEmision(),
				comprobante.getHoraEmision(), comprobante.getGravado(), comprobante.getIgv(),
				comprobante.getPrecioTotal(), comprobante.getTipoPago(), comprobante.getVueltoPago(),
				comprobante.getMontoRecibido(),
				comprobante.getCajero() != null ? comprobante.getCajero().getId() : null,
				comprobante.getPedido() != null ? comprobante.getPedido().getId() : null);
	}

}

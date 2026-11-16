package com.proyecto.restaurante.service;

import com.proyecto.restaurante.dto.ProductoVendidoResponse;
import com.proyecto.restaurante.dto.VentasResumenResponse;
import com.proyecto.restaurante.entity.DetallePedido;
import com.proyecto.restaurante.entity.Pedido;
import com.proyecto.restaurante.repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public VentasResumenResponse resumen(LocalDate desdeParam, LocalDate hastaParam) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = (desdeParam != null) ? desdeParam : hoy.minusDays(30);
        LocalDate hasta = (hastaParam != null) ? hastaParam : hoy;

        LocalDateTime desdeTime = desde.atStartOfDay();
        LocalDateTime hastaTime = hasta.plusDays(1).atStartOfDay();

        List<Pedido> pedidos = pedidoRepository.ventasActivas(desdeTime, hastaTime);

        BigDecimal total = pedidos.stream()
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, Acumulado> acumulados = new LinkedHashMap<>();
        for (Pedido pedido : pedidos) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                Long productoId = detalle.getProducto().getId();
                Acumulado existente = acumulados.get(productoId);
                if (existente == null) {
                    acumulados.put(productoId, new Acumulado(
                            productoId,
                            detalle.getProducto().getNombre(),
                            detalle.getCantidad(),
                            detalle.getSubtotal()));
                } else {
                    acumulados.put(productoId, new Acumulado(
                            productoId,
                            existente.producto(),
                            existente.cantidadVendida() + detalle.getCantidad(),
                            existente.totalVendido().add(detalle.getSubtotal())));
                }
            }
        }

        List<ProductoVendidoResponse> top = acumulados.values().stream()
                .sorted(Comparator.comparingInt(Acumulado::cantidadVendida).reversed())
                .limit(5)
                .map(a -> new ProductoVendidoResponse(
                        a.productoId(), a.producto(), a.cantidadVendida(), a.totalVendido()))
                .toList();

        int cantidadPedidos = pedidos.size();
        BigDecimal promedio = (cantidadPedidos == 0)
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(cantidadPedidos), 2, RoundingMode.HALF_UP);

        return new VentasResumenResponse(desde, hasta, cantidadPedidos, total, promedio, top);
    }

    private record Acumulado(Long productoId, String producto, int cantidadVendida, BigDecimal totalVendido) {
    }
}

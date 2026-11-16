package com.proyecto.restaurante.service;

import com.proyecto.restaurante.dto.DetallePedidoResponse;
import com.proyecto.restaurante.dto.EstadoRequest;
import com.proyecto.restaurante.dto.ItemPedidoRequest;
import com.proyecto.restaurante.dto.PedidoRequest;
import com.proyecto.restaurante.dto.PedidoResponse;
import com.proyecto.restaurante.entity.DetallePedido;
import com.proyecto.restaurante.entity.EstadoPedido;
import com.proyecto.restaurante.entity.Pedido;
import com.proyecto.restaurante.entity.Producto;
import com.proyecto.restaurante.exception.OperacionNoPermitidaException;
import com.proyecto.restaurante.exception.RecursoNoEncontradoException;
import com.proyecto.restaurante.exception.StockInsuficienteException;
import com.proyecto.restaurante.repository.PedidoRepository;
import com.proyecto.restaurante.repository.ProductoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(EstadoPedido estado) {
        List<Pedido> pedidos = (estado == null)
                ? pedidoRepository.findAllByOrderByFechaDesc()
                : pedidoRepository.findByEstadoOrderByFechaDesc(estado);
        return pedidos.stream().map(PedidoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        return PedidoResponse.from(buscarPedido(id));
    }

    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        Pedido pedido = Pedido.builder()
                .fecha(LocalDateTime.now())
                .estado(EstadoPedido.PENDIENTE)
                .metodoPago(request.metodoPago())
                .cliente(request.cliente())
                .mesa(request.mesa())
                .total(BigDecimal.ZERO)
                .build();

        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemPedidoRequest item : request.items()) {
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
            Producto producto = productoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Producto con id " + entry.getKey() + " no encontrado"));
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new OperacionNoPermitidaException(
                        "El producto '" + producto.getNombre() + "' no está disponible");
            }
            int cantidad = entry.getValue();
            if (producto.getStock() < cantidad) {
                throw new StockInsuficienteException(
                        "Stock insuficiente para '" + producto.getNombre() + "': disponibles "
                                + producto.getStock() + ", solicitados " + cantidad);
            }
            producto.setStock(producto.getStock() - cantidad);

            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
            DetallePedido detalle = DetallePedido.builder()
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(producto.getPrecio())
                    .subtotal(subtotal)
                    .build();
            pedido.agregarDetalle(detalle);
            total = total.add(subtotal);
        }
        pedido.setTotal(total);
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoRequest request) {
        Pedido pedido = buscarPedido(id);
        EstadoPedido nuevoEstado = request.estado();

        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new OperacionNoPermitidaException(
                    "No se puede cambiar el estado de un pedido cancelado");
        }
        if (nuevoEstado == EstadoPedido.CANCELADO) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                Producto producto = detalle.getProducto();
                producto.setStock(producto.getStock() + detalle.getCantidad());
                productoRepository.save(producto);
            }
        }
        pedido.setEstado(nuevoEstado);
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido con id " + id + " no encontrado"));
    }
}

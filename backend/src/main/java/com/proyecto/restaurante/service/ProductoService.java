package com.proyecto.restaurante.service;

import com.proyecto.restaurante.dto.ProductoRequest;
import com.proyecto.restaurante.dto.ProductoResponse;
import com.proyecto.restaurante.entity.Categoria;
import com.proyecto.restaurante.entity.Producto;
import com.proyecto.restaurante.exception.OperacionNoPermitidaException;
import com.proyecto.restaurante.exception.RecursoNoEncontradoException;
import com.proyecto.restaurante.repository.CategoriaRepository;
import com.proyecto.restaurante.repository.ProductoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Long categoriaId, String buscar) {
        String termino = (buscar == null || buscar.isBlank()) ? null : buscar.trim();
        return productoRepository.buscar(categoriaId, termino).stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.from(buscarProducto(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoría con id " + request.categoriaId() + " no encontrada"));
        Producto producto = Producto.builder()
                .nombre(request.nombre().trim())
                .descripcion(request.descripcion())
                .precio(request.precio())
                .stock(request.stock())
                .activo(request.activo() == null || request.activo())
                .categoria(categoria)
                .build();
        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarProducto(id);
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoría con id " + request.categoriaId() + " no encontrada"));
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setActivo(request.activo() == null || request.activo());
        producto.setCategoria(categoria);
        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscarProducto(id);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Transactional
    public ProductoResponse actualizarStock(Long id, Integer nuevoStock) {
        if (nuevoStock < 0) {
            throw new OperacionNoPermitidaException("El stock no puede ser negativo");
        }
        Producto producto = buscarProducto(id);
        producto.setStock(nuevoStock);
        return ProductoResponse.from(productoRepository.save(producto));
    }

    private Producto buscarProducto(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto con id " + id + " no encontrado"));
    }
}

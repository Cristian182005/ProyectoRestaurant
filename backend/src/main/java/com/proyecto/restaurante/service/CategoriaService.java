package com.proyecto.restaurante.service;

import com.proyecto.restaurante.dto.CategoriaRequest;
import com.proyecto.restaurante.dto.CategoriaResponse;
import com.proyecto.restaurante.entity.Categoria;
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
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(String buscar) {
        List<Categoria> categorias = (buscar == null || buscar.isBlank())
                ? categoriaRepository.findAll()
                : categoriaRepository.findByNombreContainingIgnoreCase(buscar.trim());
        return categorias.stream().map(CategoriaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.from(buscarCategoria(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new OperacionNoPermitidaException("Ya existe una categoría con el nombre '" + nombre + "'");
        }
        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .descripcion(request.descripcion())
                .build();
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarCategoria(id);
        String nombre = request.nombre().trim();
        categoriaRepository.findByNombreIgnoreCase(nombre)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new OperacionNoPermitidaException(
                            "Ya existe otra categoría con el nombre '" + nombre + "'");
                });
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.descripcion());
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarCategoria(id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar la categoría '" + categoria.getNombre()
                            + "' porque tiene productos asociados");
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría con id " + id + " no encontrada"));
    }
}

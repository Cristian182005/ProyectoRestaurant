package com.proyecto.restaurante.repository;

import com.proyecto.restaurante.entity.Producto;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByCategoriaId(Long categoriaId);

    boolean existsByCategoriaId(Long categoriaId);

    List<Producto> findByActivoTrue();

    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    @Query("""
            SELECT p FROM Producto p
            WHERE p.activo = true
              AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId)
              AND (:buscar IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')))
            ORDER BY p.nombre
            """)
    List<Producto> buscar(@Param("categoriaId") Long categoriaId, @Param("buscar") String buscar);

    @Query("SELECT COALESCE(SUM(p.stock), 0) FROM Producto p WHERE p.activo = true")
    Integer sumarStockActual();

    long countByPrecioBetween(BigDecimal desde, BigDecimal hasta);
}

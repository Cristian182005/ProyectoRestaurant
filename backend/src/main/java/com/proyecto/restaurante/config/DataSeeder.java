package com.proyecto.restaurante.config;

import com.proyecto.restaurante.entity.Categoria;
import com.proyecto.restaurante.entity.Producto;
import com.proyecto.restaurante.repository.CategoriaRepository;
import com.proyecto.restaurante.repository.ProductoRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    @Bean
    @Profile("dev")
    public CommandLineRunner cargarDatosIniciales(
            CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        return args -> {
            if (categoriaRepository.count() > 0) {
                return;
            }

            Categoria entradas = categoriaRepository.save(
                    Categoria.builder().nombre("Entradas").descripcion("Para compartir antes del plato principal").build());
            Categoria fondos = categoriaRepository.save(
                    Categoria.builder().nombre("Platos de Fondo").descripcion("Platos principales").build());
            Categoria bebidas = categoriaRepository.save(
                    Categoria.builder().nombre("Bebidas").descripcion("Bebidas frías y calientes").build());
            Categoria postres = categoriaRepository.save(
                    Categoria.builder().nombre("Postres").descripcion("Dulces para finalizar").build());

            productoRepository.saveAll(java.util.List.of(
                    producto("Causa limeña", "Entrada fría de papa amarilla", 18.00, 30, entradas),
                    producto("Ceviche clásico", "Pesado del día con leche de tigre", 25.00, 20, entradas),
                    producto("Lomo saltado", "Con papas fritas y arroz", 32.00, 25, fondos),
                    producto("Ají de gallina", "Con arroz y papa sancochada", 26.00, 25, fondos),
                    producto("Arroz chaufa", "Estilo casa", 24.00, 30, fondos),
                    producto("Chicha morada", "Jarra 1 litro", 8.00, 40, bebidas),
                    producto("Inca Kola", "Botella 500ml", 6.00, 50, bebidas),
                    producto("Café con leche", "Grano seleccionado", 7.00, 60, bebidas),
                    producto("Mazamorra morada", "Postre tradicional", 9.00, 20, postres),
                    producto("Tres leches", "Porción individual", 12.00, 15, postres)));
        };
    }

    private Producto producto(String nombre, String descripcion, double precio, int stock, Categoria categoria) {
        return Producto.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .precio(BigDecimal.valueOf(precio))
                .stock(stock)
                .activo(true)
                .categoria(categoria)
                .build();
    }
}

package com.elhornero.backend.repository;

import com.elhornero.backend.entity.Comprobante;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Integer> {

	Optional<Comprobante> findByPedidoId(Integer pedidoId);

	boolean existsByPedidoId(Integer pedidoId);

	boolean existsByCajeroId(Integer cajeroId);

	@Query("select c from Comprobante c where c.fechaEmision between :desde and :hasta "
			+ "order by c.fechaEmision desc, c.horaEmision desc")
	List<Comprobante> listarEntreFechas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

	@Query("select coalesce(sum(c.precioTotal), 0) from Comprobante c "
			+ "where c.fechaEmision between :desde and :hasta")
	Double sumarTotalEntreFechas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

	@Query("select coalesce(sum(c.igv), 0) from Comprobante c "
			+ "where c.fechaEmision between :desde and :hasta")
	Double sumarIgvEntreFechas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

}

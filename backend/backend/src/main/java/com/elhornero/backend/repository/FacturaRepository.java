package com.elhornero.backend.repository;

import com.elhornero.backend.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {

}

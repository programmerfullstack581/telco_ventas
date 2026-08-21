package com.telco.ventas.repository;

import com.telco.ventas.entity.Distrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistritoRepository extends JpaRepository<Distrito, Long> {
    List<Distrito> findByActivoTrueOrderByNombreAsc();
    List<Distrito> findByDepartamentoOrderByNombreAsc(String departamento);
}

package com.telco.ventas.repository;

import com.telco.ventas.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<Plan> findByActivoTrueOrderByTipoAscNombreAsc();
    List<Plan> findAllByOrderByIdDesc();
}

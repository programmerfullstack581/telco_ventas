package com.telco.ventas.repository;

import com.telco.ventas.entity.Comision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComisionRepository extends JpaRepository<Comision, Long> {
    Optional<Comision> findByVentaId(Long ventaId);
    List<Comision> findByAgenteId(Long agenteId);
    List<Comision> findByAgenteIdInOrderByFechaCalculoDesc(List<Long> agenteIds);
    void deleteByAgenteId(Long agenteId);

    @Query("SELECT COALESCE(SUM(c.montoComision), 0) FROM Comision c WHERE c.agenteId IN :agenteIds AND c.estado = 'PENDIENTE'")
    BigDecimal sumPendientes(@Param("agenteIds") List<Long> agenteIds);

    @Query("SELECT COALESCE(SUM(c.montoComision), 0) FROM Comision c WHERE c.agenteId IN :agenteIds AND c.estado = 'PAGADA'")
    BigDecimal sumPagadas(@Param("agenteIds") List<Long> agenteIds);

    @Query("SELECT c.agenteId as agenteId, COUNT(c) as cantidad, " +
            "SUM(CASE WHEN c.estado = 'PENDIENTE' THEN 1 ELSE 0 END) as pendientes, " +
            "SUM(CASE WHEN c.estado = 'PAGADA' THEN 1 ELSE 0 END) as pagadas, " +
            "COALESCE(SUM(CASE WHEN c.estado = 'PENDIENTE' THEN c.montoComision ELSE 0 END), 0) as montoPendiente, " +
            "COALESCE(SUM(CASE WHEN c.estado = 'PAGADA' THEN c.montoComision ELSE 0 END), 0) as montoPagado " +
            "FROM Comision c WHERE c.agenteId IN :agenteIds GROUP BY c.agenteId")
    List<Object[]> resumenPorAgente(@Param("agenteIds") List<Long> agenteIds);
}

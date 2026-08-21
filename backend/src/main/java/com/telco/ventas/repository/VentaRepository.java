package com.telco.ventas.repository;

import com.telco.ventas.entity.EstadoVenta;
import com.telco.ventas.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findByCodigoLlamada(String codigoLlamada);

    boolean existsByCodigoLlamada(String codigoLlamada);

    void deleteByAgenteId(Long agenteId);

    Page<Venta> findByAgenteId(Long agenteId, Pageable pageable);

    Page<Venta> findByAgenteIdAndEstado(Long agenteId, EstadoVenta estado, Pageable pageable);

    Page<Venta> findByAgenteIdAndFechaRegistroBetween(Long agenteId, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<Venta> findByAgenteIdAndEstadoAndFechaRegistroBetween(Long agenteId, EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<Venta> findByEstado(EstadoVenta estado, Pageable pageable);

    List<Venta> findByAgenteIdIn(List<Long> agenteIds);

    List<Venta> findByAgenteIdInAndEstado(List<Long> agenteIds, EstadoVenta estado);

    List<Venta> findByAgenteIdInAndFechaRegistroBetween(List<Long> agenteIds, LocalDateTime desde, LocalDateTime hasta);

    List<Venta> findByAgenteIdInAndEstadoAndFechaRegistroBetween(List<Long> agenteIds, EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.agenteId IN :agenteIds AND v.estado = :estado")
    Long countByAgenteIdInAndEstado(@Param("agenteIds") List<Long> agenteIds, @Param("estado") EstadoVenta estado);

    @Query("SELECT COALESCE(SUM(v.monto), 0) FROM Venta v WHERE v.agenteId IN :agenteIds AND v.estado = 'APROBADA'")
    BigDecimal sumMontoAprobadas(@Param("agenteIds") List<Long> agenteIds);

    @Query("SELECT CAST(v.fechaRegistro AS date) as fecha, COUNT(v) as cantidad, COALESCE(SUM(v.monto), 0) as monto " +
            "FROM Venta v WHERE v.agenteId IN :agenteIds AND v.fechaRegistro BETWEEN :desde AND :hasta " +
            "GROUP BY CAST(v.fechaRegistro AS date) ORDER BY fecha")
    List<Object[]> ventasPorDia(@Param("agenteIds") List<Long> agenteIds,
                                @Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(v.planId, -1) as planId, COUNT(v) as cantidad, " +
            "SUM(CASE WHEN v.estado = 'APROBADA' THEN 1 ELSE 0 END) as aprobadas, " +
            "COALESCE(SUM(CASE WHEN v.estado = 'APROBADA' THEN v.monto ELSE 0 END), 0) as montoAprobado " +
            "FROM Venta v WHERE v.agenteId IN :agenteIds AND v.fechaRegistro BETWEEN :desde AND :hasta " +
            "GROUP BY COALESCE(v.planId, -1) ORDER BY cantidad DESC")
    List<Object[]> ventasPorPlan(@Param("agenteIds") List<Long> agenteIds,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("hasta") LocalDateTime hasta);

    @Query("SELECT v.agenteId as agenteId, COUNT(v) as cantidad, " +
            "SUM(CASE WHEN v.estado = 'PENDIENTE' THEN 1 ELSE 0 END) as pendientes, " +
            "SUM(CASE WHEN v.estado = 'APROBADA' THEN 1 ELSE 0 END) as aprobadas, " +
            "SUM(CASE WHEN v.estado = 'RECHAZADA' THEN 1 ELSE 0 END) as rechazadas, " +
            "COALESCE(SUM(CASE WHEN v.estado = 'APROBADA' THEN v.monto ELSE 0 END), 0) as montoAprobado " +
            "FROM Venta v WHERE v.agenteId IN :agenteIds AND v.fechaRegistro BETWEEN :desde AND :hasta " +
            "GROUP BY v.agenteId ORDER BY cantidad DESC")
    List<Object[]> ventasPorAgente(@Param("agenteIds") List<Long> agenteIds,
                                   @Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta);
}

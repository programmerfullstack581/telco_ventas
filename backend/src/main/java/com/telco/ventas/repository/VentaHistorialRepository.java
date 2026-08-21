package com.telco.ventas.repository;

import com.telco.ventas.entity.VentaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VentaHistorialRepository extends JpaRepository<VentaHistorial, Long> {
    List<VentaHistorial> findByVentaIdOrderByFechaAsc(Long ventaId);
    void deleteByUsuarioId(Long usuarioId);
}

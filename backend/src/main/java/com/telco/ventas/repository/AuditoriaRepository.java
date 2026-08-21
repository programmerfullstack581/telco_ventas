package com.telco.ventas.repository;

import com.telco.ventas.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    Page<Auditoria> findByAccionContainingIgnoreCaseAndUsuarioUsernameContainingIgnoreCase(
            String accion, String username, Pageable pageable);

    Page<Auditoria> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<Auditoria> findByAccionContainingIgnoreCaseAndUsuarioUsernameContainingIgnoreCaseAndFechaBetween(
            String accion, String username, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    void deleteByUsuarioId(Long usuarioId);
}

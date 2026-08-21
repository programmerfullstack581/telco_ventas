package com.telco.ventas.repository;

import com.telco.ventas.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByDni(String dni);
    boolean existsByDni(String dni);
    Page<Cliente> findByDniContainingIgnoreCaseOrNombreClienteContainingIgnoreCase(String dni, String nombre, Pageable pageable);
}

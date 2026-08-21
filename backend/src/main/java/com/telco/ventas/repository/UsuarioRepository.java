package com.telco.ventas.repository;

import com.telco.ventas.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
    List<Usuario> findBySupervisorId(Long supervisorId);
    List<Usuario> findByRolId(Long rolId);
    long countByRolId(Long rolId);
    List<Usuario> findAllByOrderByIdDesc();

    @Query("SELECT u FROM Usuario u JOIN Rol r ON r.id = u.rolId " +
            "WHERE r.nombre IN ('AGENTE', 'SUPERVISOR') ORDER BY u.username")
    List<Usuario> findAgentesYSupervisores();

    @Query("SELECT r.nombre FROM Usuario u JOIN Rol r ON r.id = u.rolId WHERE u.id = :usuarioId")
    Optional<String> findRolNombreByUsuarioId(Long usuarioId);
}

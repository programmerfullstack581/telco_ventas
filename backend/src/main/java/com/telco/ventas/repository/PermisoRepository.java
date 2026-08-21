package com.telco.ventas.repository;

import com.telco.ventas.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    List<Permiso> findByCodigoIn(Collection<String> codigos);
    List<Permiso> findAllByOrderByModuloAscAccionAsc();
}

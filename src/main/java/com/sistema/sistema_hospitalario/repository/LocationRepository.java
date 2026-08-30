package com.sistema.sistema_hospitalario.repository;

import com.sistema.sistema_hospitalario.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}

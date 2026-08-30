package com.sistema.sistema_hospitalario.repository;

import com.sistema.sistema_hospitalario.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserId(Long userId);
    Optional<Patient> findByNationalId(String nationalId);
    boolean existsByNationalId(String nationalId);
}

package com.sistema.sistema_hospitalario.repository;

import com.sistema.sistema_hospitalario.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findBySpecialtyId(Long specialtyId);
    List<Doctor> findByLocationId(Long locationId);
    Optional<Doctor> findByProfessionalId(String professionalId);
}

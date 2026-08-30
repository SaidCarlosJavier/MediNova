package com.sistema.sistema_hospitalario.repository;

import com.sistema.sistema_hospitalario.entity.Appointment;
import com.sistema.sistema_hospitalario.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findByDoctorIdAndDate(Long doctorId, LocalDate date);
    List<Appointment> findByStatus(AppointmentStatus status);
    List<Appointment> findByDate(LocalDate date);
    boolean existsByDoctorIdAndDateAndTime(Long doctorId, LocalDate date, LocalTime time);
}

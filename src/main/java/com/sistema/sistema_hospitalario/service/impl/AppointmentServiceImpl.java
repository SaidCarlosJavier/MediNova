package com.sistema.sistema_hospitalario.service.impl;

import com.sistema.sistema_hospitalario.dto.request.AppointmentRequest;
import com.sistema.sistema_hospitalario.dto.response.AppointmentResponse;
import com.sistema.sistema_hospitalario.entity.Appointment;
import com.sistema.sistema_hospitalario.entity.Doctor;
import com.sistema.sistema_hospitalario.entity.Location;
import com.sistema.sistema_hospitalario.entity.Patient;
import com.sistema.sistema_hospitalario.entity.Specialty;
import com.sistema.sistema_hospitalario.enums.AppointmentStatus;
import com.sistema.sistema_hospitalario.exception.BadRequestException;
import com.sistema.sistema_hospitalario.exception.ResourceNotFoundException;
import com.sistema.sistema_hospitalario.repository.AppointmentRepository;
import com.sistema.sistema_hospitalario.repository.DoctorRepository;
import com.sistema.sistema_hospitalario.repository.LocationRepository;
import com.sistema.sistema_hospitalario.repository.PatientRepository;
import com.sistema.sistema_hospitalario.repository.SpecialtyRepository;
import com.sistema.sistema_hospitalario.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        Appointment appointment = new Appointment();
        applyRequest(appointment, request, true);
        return toResponse(appointmentRepository.save(appointment));
    }

    @Override
    public AppointmentResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<AppointmentResponse> getAll() {
        return appointmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<AppointmentResponse> getByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<AppointmentResponse> getByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public AppointmentResponse update(Long id, AppointmentRequest request) {
        Appointment appointment = findEntity(id);
        applyRequest(appointment, request, false);
        return toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse changeStatus(Long id, AppointmentStatus status) {
        Appointment appointment = findEntity(id);
        appointment.setStatus(status);
        return toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        appointmentRepository.delete(findEntity(id));
    }

    private void applyRequest(Appointment appointment, AppointmentRequest request, boolean isCreate) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.getPatientId()));
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.getDoctorId()));

        boolean slotChanged = isCreate
                || !appointment.getDoctor().getId().equals(doctor.getId())
                || !appointment.getDate().equals(request.getDate())
                || !appointment.getTime().equals(request.getTime());

        if (slotChanged && appointmentRepository.existsByDoctorIdAndDateAndTime(
                doctor.getId(), request.getDate(), request.getTime())) {
            throw new BadRequestException("El doctor ya tiene una cita agendada en esa fecha y hora");
        }

        if (doctor.getBlockedDates() != null && doctor.getBlockedDates().contains(request.getDate())) {
            throw new BadRequestException("El doctor no atiende en la fecha seleccionada");
        }

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setDate(request.getDate());
        appointment.setTime(request.getTime());
        appointment.setType(request.getType());
        appointment.setReason(request.getReason());

        if (request.getSpecialtyId() != null) {
            Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad", "id", request.getSpecialtyId()));
            appointment.setSpecialty(specialty);
        } else {
            appointment.setSpecialty(doctor.getSpecialty());
        }

        if (request.getLocationId() != null) {
            Location location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sede", "id", request.getLocationId()));
            appointment.setLocation(location);
        } else if (doctor.getLocation() != null) {
            appointment.setLocation(doctor.getLocation());
        }

        if (isCreate) {
            appointment.setStatus(AppointmentStatus.PENDING);
        }
    }

    private Appointment findEntity(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", id));
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getName())
                .doctorId(a.getDoctor().getId())
                .doctorName(a.getDoctor().getName())
                .specialtyId(a.getSpecialty() != null ? a.getSpecialty().getId() : null)
                .specialtyName(a.getSpecialty() != null ? a.getSpecialty().getName() : null)
                .date(a.getDate())
                .time(a.getTime())
                .type(a.getType())
                .locationId(a.getLocation() != null ? a.getLocation().getId() : null)
                .locationName(a.getLocation() != null ? a.getLocation().getName() : null)
                .reason(a.getReason())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }
}

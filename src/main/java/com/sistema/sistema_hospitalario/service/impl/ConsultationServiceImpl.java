package com.sistema.sistema_hospitalario.service.impl;

import com.sistema.sistema_hospitalario.dto.request.ConsultationRequest;
import com.sistema.sistema_hospitalario.dto.response.ConsultationResponse;
import com.sistema.sistema_hospitalario.dto.response.VitalsResponse;
import com.sistema.sistema_hospitalario.entity.*;
import com.sistema.sistema_hospitalario.enums.AppointmentStatus;
import com.sistema.sistema_hospitalario.exception.ResourceNotFoundException;
import com.sistema.sistema_hospitalario.repository.*;
import com.sistema.sistema_hospitalario.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public ConsultationResponse create(ConsultationRequest request) {
        Consultation consultation = new Consultation();
        consultation.setDate(LocalDateTime.now());
        applyRequest(consultation, request);
        Consultation saved = consultationRepository.save(consultation);

        if (request.getAppointmentId() != null) {
            appointmentRepository.findById(request.getAppointmentId()).ifPresent(appt -> {
                appt.setStatus(AppointmentStatus.COMPLETED);
                appointmentRepository.save(appt);
            });
        }

        return toResponse(saved);
    }

    @Override
    public ConsultationResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<ConsultationResponse> getAll() {
        return consultationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<ConsultationResponse> getByPatient(Long patientId) {
        return consultationRepository.findByPatientId(patientId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<ConsultationResponse> getByDoctor(Long doctorId) {
        return consultationRepository.findByDoctorId(doctorId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ConsultationResponse update(Long id, ConsultationRequest request) {
        Consultation consultation = findEntity(id);
        applyRequest(consultation, request);
        return toResponse(consultationRepository.save(consultation));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        consultationRepository.delete(findEntity(id));
    }

    private void applyRequest(Consultation consultation, ConsultationRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.getPatientId()));
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.getDoctorId()));

        consultation.setPatient(patient);
        consultation.setDoctor(doctor);

        if (request.getSpecialtyId() != null) {
            Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad", "id", request.getSpecialtyId()));
            consultation.setSpecialty(specialty);
        } else {
            consultation.setSpecialty(doctor.getSpecialty());
        }

        consultation.setReason(request.getReason());
        consultation.setSymptoms(request.getSymptoms());

        if (request.getVitals() != null) {
            consultation.setVitals(Vitals.builder()
                    .weight(request.getVitals().getWeight())
                    .height(request.getVitals().getHeight())
                    .bp(request.getVitals().getBp())
                    .temp(request.getVitals().getTemp())
                    .hr(request.getVitals().getHr())
                    .spo2(request.getVitals().getSpo2())
                    .build());
        }

        consultation.setDiagnosis(request.getDiagnosis());
        consultation.setTreatment(request.getTreatment());
        consultation.setMedications(request.getMedications() != null ? request.getMedications() : new ArrayList<>());
        consultation.setRecommendations(request.getRecommendations());
        consultation.setNotes(request.getNotes());
        consultation.setFollowUp(request.getFollowUp());
    }

    private Consultation findEntity(Long id) {
        return consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta", "id", id));
    }

    private ConsultationResponse toResponse(Consultation c) {
        VitalsResponse vitals = c.getVitals() == null ? null : VitalsResponse.builder()
                .weight(c.getVitals().getWeight())
                .height(c.getVitals().getHeight())
                .bp(c.getVitals().getBp())
                .temp(c.getVitals().getTemp())
                .hr(c.getVitals().getHr())
                .spo2(c.getVitals().getSpo2())
                .build();

        return ConsultationResponse.builder()
                .id(c.getId())
                .patientId(c.getPatient().getId())
                .patientName(c.getPatient().getName())
                .doctorId(c.getDoctor().getId())
                .doctorName(c.getDoctor().getName())
                .specialtyId(c.getSpecialty() != null ? c.getSpecialty().getId() : null)
                .specialtyName(c.getSpecialty() != null ? c.getSpecialty().getName() : null)
                .date(c.getDate())
                .reason(c.getReason())
                .symptoms(c.getSymptoms())
                .vitals(vitals)
                .diagnosis(c.getDiagnosis())
                .treatment(c.getTreatment())
                .medications(c.getMedications())
                .recommendations(c.getRecommendations())
                .notes(c.getNotes())
                .followUp(c.getFollowUp())
                .build();
    }
}

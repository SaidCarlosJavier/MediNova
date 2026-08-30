package com.sistema.sistema_hospitalario.service.impl;

import com.sistema.sistema_hospitalario.dto.request.PatientRequest;
import com.sistema.sistema_hospitalario.dto.response.PatientResponse;
import com.sistema.sistema_hospitalario.entity.Patient;
import com.sistema.sistema_hospitalario.entity.User;
import com.sistema.sistema_hospitalario.enums.AccountStatus;
import com.sistema.sistema_hospitalario.exception.DuplicateResourceException;
import com.sistema.sistema_hospitalario.exception.ResourceNotFoundException;
import com.sistema.sistema_hospitalario.repository.PatientRepository;
import com.sistema.sistema_hospitalario.repository.UserRepository;
import com.sistema.sistema_hospitalario.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PatientResponse create(PatientRequest request) {
        if (patientRepository.existsByNationalId(request.getNationalId())) {
            throw new DuplicateResourceException("Ya existe un paciente con el documento: " + request.getNationalId());
        }

        Patient patient = new Patient();
        applyRequest(patient, request);

        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.getUserId()));
            patient.setUser(user);
        }

        return toResponse(patientRepository.save(patient));
    }

    @Override
    public PatientResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<PatientResponse> getAll() {
        return patientRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = findEntity(id);

        if (!patient.getNationalId().equalsIgnoreCase(request.getNationalId())
                && patientRepository.existsByNationalId(request.getNationalId())) {
            throw new DuplicateResourceException("Ya existe un paciente con el documento: " + request.getNationalId());
        }

        applyRequest(patient, request);
        return toResponse(patientRepository.save(patient));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        patientRepository.delete(findEntity(id));
    }

    private void applyRequest(Patient patient, PatientRequest request) {
        patient.setName(request.getName());
        patient.setNationalId(request.getNationalId());
        patient.setDob(request.getDob());
        patient.setGender(request.getGender());
        patient.setPhone(request.getPhone());
        patient.setEmail(request.getEmail());
        patient.setAddress(request.getAddress());
        patient.setPhoto(request.getPhoto());
        if (request.getStatus() != null) {
            patient.setStatus(request.getStatus());
        } else if (patient.getStatus() == null) {
            patient.setStatus(AccountStatus.ACTIVE);
        }
        patient.setAllergies(request.getAllergies() != null ? request.getAllergies() : new ArrayList<>());
        patient.setBloodType(request.getBloodType());
    }

    private Patient findEntity(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", id));
    }

    private PatientResponse toResponse(Patient p) {
        return PatientResponse.builder()
                .id(p.getId())
                .userId(p.getUser() != null ? p.getUser().getId() : null)
                .name(p.getName())
                .nationalId(p.getNationalId())
                .dob(p.getDob())
                .gender(p.getGender())
                .phone(p.getPhone())
                .email(p.getEmail())
                .address(p.getAddress())
                .photo(p.getPhoto())
                .registered(p.getRegistered())
                .status(p.getStatus())
                .allergies(p.getAllergies())
                .bloodType(p.getBloodType())
                .build();
    }
}

package com.sistema.sistema_hospitalario.service.impl;

import com.sistema.sistema_hospitalario.dto.request.MedicalDocumentRequest;
import com.sistema.sistema_hospitalario.dto.response.MedicalDocumentResponse;
import com.sistema.sistema_hospitalario.entity.Doctor;
import com.sistema.sistema_hospitalario.entity.MedicalDocument;
import com.sistema.sistema_hospitalario.entity.Patient;
import com.sistema.sistema_hospitalario.enums.DocumentStatus;
import com.sistema.sistema_hospitalario.exception.ResourceNotFoundException;
import com.sistema.sistema_hospitalario.repository.DoctorRepository;
import com.sistema.sistema_hospitalario.repository.MedicalDocumentRepository;
import com.sistema.sistema_hospitalario.repository.PatientRepository;
import com.sistema.sistema_hospitalario.service.MedicalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalDocumentServiceImpl implements MedicalDocumentService {

    private final MedicalDocumentRepository documentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    @Override
    @Transactional
    public MedicalDocumentResponse create(MedicalDocumentRequest request) {
        MedicalDocument document = new MedicalDocument();
        applyRequest(document, request);
        document.setStatus(DocumentStatus.AVAILABLE);
        return toResponse(documentRepository.save(document));
    }

    @Override
    public MedicalDocumentResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<MedicalDocumentResponse> getAll() {
        return documentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<MedicalDocumentResponse> getByPatient(Long patientId) {
        return documentRepository.findByPatientId(patientId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public MedicalDocumentResponse update(Long id, MedicalDocumentRequest request) {
        MedicalDocument document = findEntity(id);
        applyRequest(document, request);
        return toResponse(documentRepository.save(document));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        documentRepository.delete(findEntity(id));
    }

    private void applyRequest(MedicalDocument document, MedicalDocumentRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.getPatientId()));
        document.setPatient(patient);

        if (request.getDoctorId() != null) {
            Doctor doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.getDoctorId()));
            document.setDoctor(doctor);
        }

        document.setName(request.getName());
        document.setType(request.getType());
        document.setSize(request.getSize());
    }

    private MedicalDocument findEntity(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento", "id", id));
    }

    private MedicalDocumentResponse toResponse(MedicalDocument d) {
        return MedicalDocumentResponse.builder()
                .id(d.getId())
                .patientId(d.getPatient().getId())
                .patientName(d.getPatient().getName())
                .doctorId(d.getDoctor() != null ? d.getDoctor().getId() : null)
                .doctorName(d.getDoctor() != null ? d.getDoctor().getName() : null)
                .name(d.getName())
                .type(d.getType())
                .date(d.getDate())
                .size(d.getSize())
                .status(d.getStatus())
                .build();
    }
}

package com.sistema.sistema_hospitalario.service;

import com.sistema.sistema_hospitalario.dto.request.ConsultationRequest;
import com.sistema.sistema_hospitalario.dto.response.ConsultationResponse;

import java.util.List;

public interface ConsultationService {
    ConsultationResponse create(ConsultationRequest request);
    ConsultationResponse getById(Long id);
    List<ConsultationResponse> getAll();
    List<ConsultationResponse> getByPatient(Long patientId);
    List<ConsultationResponse> getByDoctor(Long doctorId);
    ConsultationResponse update(Long id, ConsultationRequest request);
    void delete(Long id);
}

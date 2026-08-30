package com.sistema.sistema_hospitalario.service;

import com.sistema.sistema_hospitalario.dto.request.PatientRequest;
import com.sistema.sistema_hospitalario.dto.response.PatientResponse;

import java.util.List;

public interface PatientService {
    PatientResponse create(PatientRequest request);
    PatientResponse getById(Long id);
    List<PatientResponse> getAll();
    PatientResponse update(Long id, PatientRequest request);
    void delete(Long id);
}

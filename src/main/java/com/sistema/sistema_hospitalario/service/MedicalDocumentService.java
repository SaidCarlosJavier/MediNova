package com.sistema.sistema_hospitalario.service;

import com.sistema.sistema_hospitalario.dto.request.MedicalDocumentRequest;
import com.sistema.sistema_hospitalario.dto.response.MedicalDocumentResponse;

import java.util.List;

public interface MedicalDocumentService {
    MedicalDocumentResponse create(MedicalDocumentRequest request);
    MedicalDocumentResponse getById(Long id);
    List<MedicalDocumentResponse> getAll();
    List<MedicalDocumentResponse> getByPatient(Long patientId);
    MedicalDocumentResponse update(Long id, MedicalDocumentRequest request);
    void delete(Long id);
}

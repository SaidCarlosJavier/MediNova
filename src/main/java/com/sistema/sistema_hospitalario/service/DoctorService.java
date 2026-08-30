package com.sistema.sistema_hospitalario.service;

import com.sistema.sistema_hospitalario.dto.request.DoctorRequest;
import com.sistema.sistema_hospitalario.dto.response.DoctorResponse;

import java.util.List;

public interface DoctorService {
    DoctorResponse create(DoctorRequest request);
    DoctorResponse getById(Long id);
    List<DoctorResponse> getAll();
    List<DoctorResponse> getBySpecialty(Long specialtyId);
    DoctorResponse update(Long id, DoctorRequest request);
    void delete(Long id);
}

package com.sistema.sistema_hospitalario.service;

import com.sistema.sistema_hospitalario.dto.request.SpecialtyRequest;
import com.sistema.sistema_hospitalario.dto.response.SpecialtyResponse;

import java.util.List;

public interface SpecialtyService {
    SpecialtyResponse create(SpecialtyRequest request);
    SpecialtyResponse getById(Long id);
    List<SpecialtyResponse> getAll();
    SpecialtyResponse update(Long id, SpecialtyRequest request);
    void delete(Long id);
}

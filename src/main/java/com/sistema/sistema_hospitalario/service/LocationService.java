package com.sistema.sistema_hospitalario.service;

import com.sistema.sistema_hospitalario.dto.request.LocationRequest;
import com.sistema.sistema_hospitalario.dto.response.LocationResponse;

import java.util.List;

public interface LocationService {
    LocationResponse create(LocationRequest request);
    LocationResponse getById(Long id);
    List<LocationResponse> getAll();
    LocationResponse update(Long id, LocationRequest request);
    void delete(Long id);
}

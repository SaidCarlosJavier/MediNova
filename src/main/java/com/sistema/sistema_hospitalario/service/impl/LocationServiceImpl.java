package com.sistema.sistema_hospitalario.service.impl;

import com.sistema.sistema_hospitalario.dto.request.LocationRequest;
import com.sistema.sistema_hospitalario.dto.response.LocationResponse;
import com.sistema.sistema_hospitalario.entity.Location;
import com.sistema.sistema_hospitalario.exception.ResourceNotFoundException;
import com.sistema.sistema_hospitalario.repository.LocationRepository;
import com.sistema.sistema_hospitalario.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public LocationResponse create(LocationRequest request) {
        Location location = Location.builder()
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .rooms(request.getRooms() != null ? request.getRooms() : new ArrayList<>())
                .build();
        return toResponse(locationRepository.save(location));
    }

    @Override
    public LocationResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<LocationResponse> getAll() {
        return locationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public LocationResponse update(Long id, LocationRequest request) {
        Location location = findEntity(id);
        location.setName(request.getName());
        location.setAddress(request.getAddress());
        location.setPhone(request.getPhone());
        if (request.getRooms() != null) {
            location.setRooms(request.getRooms());
        }
        return toResponse(locationRepository.save(location));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        locationRepository.delete(findEntity(id));
    }

    private Location findEntity(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede", "id", id));
    }

    private LocationResponse toResponse(Location l) {
        return LocationResponse.builder()
                .id(l.getId())
                .name(l.getName())
                .address(l.getAddress())
                .phone(l.getPhone())
                .rooms(l.getRooms())
                .build();
    }
}

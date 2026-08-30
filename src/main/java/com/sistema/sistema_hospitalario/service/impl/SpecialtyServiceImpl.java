package com.sistema.sistema_hospitalario.service.impl;

import com.sistema.sistema_hospitalario.dto.request.SpecialtyRequest;
import com.sistema.sistema_hospitalario.dto.response.SpecialtyResponse;
import com.sistema.sistema_hospitalario.entity.Specialty;
import com.sistema.sistema_hospitalario.enums.AccountStatus;
import com.sistema.sistema_hospitalario.exception.DuplicateResourceException;
import com.sistema.sistema_hospitalario.exception.ResourceNotFoundException;
import com.sistema.sistema_hospitalario.repository.SpecialtyRepository;
import com.sistema.sistema_hospitalario.service.SpecialtyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialtyServiceImpl implements SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    @Override
    @Transactional
    public SpecialtyResponse create(SpecialtyRequest request) {
        if (specialtyRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Ya existe una especialidad llamada: " + request.getName());
        }
        Specialty specialty = Specialty.builder()
                .name(request.getName())
                .icon(request.getIcon())
                .description(request.getDescription())
                .doctorCount(0)
                .status(request.getStatus() != null ? request.getStatus() : AccountStatus.ACTIVE)
                .build();
        return toResponse(specialtyRepository.save(specialty));
    }

    @Override
    public SpecialtyResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<SpecialtyResponse> getAll() {
        return specialtyRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public SpecialtyResponse update(Long id, SpecialtyRequest request) {
        Specialty specialty = findEntity(id);
        specialty.setName(request.getName());
        specialty.setIcon(request.getIcon());
        specialty.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            specialty.setStatus(request.getStatus());
        }
        return toResponse(specialtyRepository.save(specialty));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        specialtyRepository.delete(findEntity(id));
    }

    private Specialty findEntity(Long id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad", "id", id));
    }

    private SpecialtyResponse toResponse(Specialty s) {
        return SpecialtyResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .icon(s.getIcon())
                .description(s.getDescription())
                .doctorCount(s.getDoctorCount())
                .status(s.getStatus())
                .build();
    }
}

package com.sistema.sistema_hospitalario.controller;

import com.sistema.sistema_hospitalario.dto.common.ApiResponse;
import com.sistema.sistema_hospitalario.dto.request.SpecialtyRequest;
import com.sistema.sistema_hospitalario.dto.response.SpecialtyResponse;
import com.sistema.sistema_hospitalario.service.SpecialtyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
@RequiredArgsConstructor
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SpecialtyResponse>> create(@Valid @RequestBody SpecialtyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Especialidad creada correctamente", specialtyService.create(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SpecialtyResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(specialtyService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SpecialtyResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(specialtyService.getAll()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SpecialtyResponse>> update(@PathVariable Long id, @Valid @RequestBody SpecialtyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Especialidad actualizada correctamente", specialtyService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        specialtyService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Especialidad eliminada correctamente", null));
    }
}

package com.sistema.sistema_hospitalario.controller;

import com.sistema.sistema_hospitalario.dto.common.ApiResponse;
import com.sistema.sistema_hospitalario.dto.request.DoctorRequest;
import com.sistema.sistema_hospitalario.dto.response.DoctorResponse;
import com.sistema.sistema_hospitalario.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DoctorResponse>> create(@Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Doctor creado correctamente", doctorService.create(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAll(
            @RequestParam(required = false) Long specialtyId) {
        List<DoctorResponse> doctors = specialtyId != null
                ? doctorService.getBySpecialty(specialtyId)
                : doctorService.getAll();
        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<ApiResponse<DoctorResponse>> update(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Doctor actualizado correctamente", doctorService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor eliminado correctamente", null));
    }
}

package com.sistema.sistema_hospitalario.controller;

import com.sistema.sistema_hospitalario.dto.common.ApiResponse;
import com.sistema.sistema_hospitalario.dto.request.LocationRequest;
import com.sistema.sistema_hospitalario.dto.response.LocationResponse;
import com.sistema.sistema_hospitalario.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> create(@Valid @RequestBody LocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sede creada correctamente", locationService.create(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getAll()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> update(@PathVariable Long id, @Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Sede actualizada correctamente", locationService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Sede eliminada correctamente", null));
    }
}

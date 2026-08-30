package com.sistema.sistema_hospitalario.controller;

import com.sistema.sistema_hospitalario.dto.common.ApiResponse;
import com.sistema.sistema_hospitalario.dto.request.MedicalDocumentRequest;
import com.sistema.sistema_hospitalario.dto.response.MedicalDocumentResponse;
import com.sistema.sistema_hospitalario.service.MedicalDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT', 'RECEPTIONIST')")
public class MedicalDocumentController {

    private final MedicalDocumentService documentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<MedicalDocumentResponse>> create(@Valid @RequestBody MedicalDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Documento registrado correctamente", documentService.create(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicalDocumentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<MedicalDocumentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(documentService.getAll()));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<MedicalDocumentResponse>>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getByPatient(patientId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<MedicalDocumentResponse>> update(@PathVariable Long id, @Valid @RequestBody MedicalDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Documento actualizado correctamente", documentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Documento eliminado correctamente", null));
    }
}

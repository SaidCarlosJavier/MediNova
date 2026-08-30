package com.sistema.sistema_hospitalario.controller;

import com.sistema.sistema_hospitalario.dto.common.ApiResponse;
import com.sistema.sistema_hospitalario.dto.response.AuditLogResponse;
import com.sistema.sistema_hospitalario.enums.AuditModule;
import com.sistema.sistema_hospitalario.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAll(
            @RequestParam(required = false) AuditModule module) {
        List<AuditLogResponse> logs = module != null
                ? auditLogService.getByModule(module)
                : auditLogService.getAll();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}

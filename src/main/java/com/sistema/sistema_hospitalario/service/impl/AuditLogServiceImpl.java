package com.sistema.sistema_hospitalario.service.impl;

import com.sistema.sistema_hospitalario.dto.response.AuditLogResponse;
import com.sistema.sistema_hospitalario.entity.AuditLog;
import com.sistema.sistema_hospitalario.enums.AuditModule;
import com.sistema.sistema_hospitalario.enums.AuditStatus;
import com.sistema.sistema_hospitalario.repository.AuditLogRepository;
import com.sistema.sistema_hospitalario.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public AuditLogResponse log(String user, String action, AuditModule module, AuditStatus status) {
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .module(module)
                .status(status)
                .build();
        return toResponse(auditLogRepository.save(auditLog));
    }

    @Override
    public List<AuditLogResponse> getAll() {
        return auditLogRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<AuditLogResponse> getByModule(AuditModule module) {
        return auditLogRepository.findByModule(module).stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .user(a.getUser())
                .action(a.getAction())
                .module(a.getModule())
                .date(a.getDate())
                .time(a.getTime())
                .status(a.getStatus())
                .build();
    }
}

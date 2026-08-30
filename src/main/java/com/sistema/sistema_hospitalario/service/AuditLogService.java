package com.sistema.sistema_hospitalario.service;

import com.sistema.sistema_hospitalario.dto.response.AuditLogResponse;
import com.sistema.sistema_hospitalario.enums.AuditModule;
import com.sistema.sistema_hospitalario.enums.AuditStatus;

import java.util.List;

public interface AuditLogService {
    AuditLogResponse log(String user, String action, AuditModule module, AuditStatus status);
    List<AuditLogResponse> getAll();
    List<AuditLogResponse> getByModule(AuditModule module);
}

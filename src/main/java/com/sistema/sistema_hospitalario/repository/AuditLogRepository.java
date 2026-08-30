package com.sistema.sistema_hospitalario.repository;

import com.sistema.sistema_hospitalario.entity.AuditLog;
import com.sistema.sistema_hospitalario.enums.AuditModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByModule(AuditModule module);
    List<AuditLog> findByUserOrderByDateDescTimeDesc(String user);
}

package com.sistema.sistema_hospitalario.repository;

import com.sistema.sistema_hospitalario.entity.MedicalDocument;
import com.sistema.sistema_hospitalario.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, Long> {
    List<MedicalDocument> findByPatientId(Long patientId);
    List<MedicalDocument> findByDoctorId(Long doctorId);
    List<MedicalDocument> findByPatientIdAndType(Long patientId, DocumentType type);
}

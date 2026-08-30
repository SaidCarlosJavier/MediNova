package com.sistema.sistema_hospitalario.service;

import com.sistema.sistema_hospitalario.dto.request.AppointmentRequest;
import com.sistema.sistema_hospitalario.dto.response.AppointmentResponse;
import com.sistema.sistema_hospitalario.enums.AppointmentStatus;

import java.util.List;

public interface AppointmentService {
    AppointmentResponse create(AppointmentRequest request);
    AppointmentResponse getById(Long id);
    List<AppointmentResponse> getAll();
    List<AppointmentResponse> getByPatient(Long patientId);
    List<AppointmentResponse> getByDoctor(Long doctorId);
    AppointmentResponse update(Long id, AppointmentRequest request);
    AppointmentResponse changeStatus(Long id, AppointmentStatus status);
    void delete(Long id);
}

package com.sistema.sistema_hospitalario.service.impl;

import com.sistema.sistema_hospitalario.dto.request.DoctorRequest;
import com.sistema.sistema_hospitalario.dto.request.ScheduleBlockRequest;
import com.sistema.sistema_hospitalario.dto.response.DoctorResponse;
import com.sistema.sistema_hospitalario.dto.response.ScheduleBlockResponse;
import com.sistema.sistema_hospitalario.entity.Doctor;
import com.sistema.sistema_hospitalario.entity.Location;
import com.sistema.sistema_hospitalario.entity.ScheduleBlock;
import com.sistema.sistema_hospitalario.entity.Specialty;
import com.sistema.sistema_hospitalario.entity.User;
import com.sistema.sistema_hospitalario.enums.AccountStatus;
import com.sistema.sistema_hospitalario.exception.ResourceNotFoundException;
import com.sistema.sistema_hospitalario.repository.DoctorRepository;
import com.sistema.sistema_hospitalario.repository.LocationRepository;
import com.sistema.sistema_hospitalario.repository.SpecialtyRepository;
import com.sistema.sistema_hospitalario.repository.UserRepository;
import com.sistema.sistema_hospitalario.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public DoctorResponse create(DoctorRequest request) {
        Doctor doctor = new Doctor();
        applyRequest(doctor, request, true);
        Doctor saved = doctorRepository.save(doctor);
        recalculateSpecialtyCount(saved.getSpecialty());
        return toResponse(saved);
    }

    @Override
    public DoctorResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<DoctorResponse> getAll() {
        return doctorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<DoctorResponse> getBySpecialty(Long specialtyId) {
        return doctorRepository.findBySpecialtyId(specialtyId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public DoctorResponse update(Long id, DoctorRequest request) {
        Doctor doctor = findEntity(id);
        Specialty previousSpecialty = doctor.getSpecialty();

        applyRequest(doctor, request, false);
        Doctor saved = doctorRepository.save(doctor);

        if (previousSpecialty != null && (saved.getSpecialty() == null
                || !previousSpecialty.getId().equals(saved.getSpecialty().getId()))) {
            recalculateSpecialtyCount(previousSpecialty);
        }
        recalculateSpecialtyCount(saved.getSpecialty());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Doctor doctor = findEntity(id);
        Specialty specialty = doctor.getSpecialty();
        doctorRepository.delete(doctor);
        recalculateSpecialtyCount(specialty);
    }

    private void applyRequest(Doctor doctor, DoctorRequest request, boolean isCreate) {
        if (isCreate && request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.getUserId()));
            doctor.setUser(user);
        }

        doctor.setName(request.getName());

        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad", "id", request.getSpecialtyId()));
        doctor.setSpecialty(specialty);

        doctor.setPhoto(request.getPhoto());
        doctor.setExperience(request.getExperience());
        doctor.setEducation(request.getEducation());
        doctor.setCertifications(request.getCertifications() != null ? request.getCertifications() : new ArrayList<>());
        doctor.setBio(request.getBio());
        doctor.setConsultationTypes(request.getConsultationTypes() != null ? request.getConsultationTypes() : new ArrayList<>());

        if (request.getLocationId() != null) {
            Location location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sede", "id", request.getLocationId()));
            doctor.setLocation(location);
        } else {
            doctor.setLocation(null);
        }

        doctor.setProfessionalId(request.getProfessionalId());
        doctor.setPhone(request.getPhone());
        doctor.setEmail(request.getEmail());

        if (request.getStatus() != null) {
            doctor.setStatus(request.getStatus());
        } else if (doctor.getStatus() == null) {
            doctor.setStatus(AccountStatus.ACTIVE);
        }

        if (doctor.getRating() == null) {
            doctor.setRating(0.0);
        }

        List<ScheduleBlock> schedule = new ArrayList<>();
        if (request.getSchedule() != null) {
            for (ScheduleBlockRequest sb : request.getSchedule()) {
                schedule.add(ScheduleBlock.builder().day(sb.getDay()).from(sb.getFrom()).to(sb.getTo()).build());
            }
        }
        doctor.setSchedule(schedule);

        List<LocalDate> blockedDates = new ArrayList<>();
        if (request.getBlockedDates() != null) {
            for (String d : request.getBlockedDates()) {
                blockedDates.add(LocalDate.parse(d));
            }
        }
        doctor.setBlockedDates(blockedDates);
    }

    private void recalculateSpecialtyCount(Specialty specialty) {
        if (specialty == null) return;
        long count = doctorRepository.findBySpecialtyId(specialty.getId()).size();
        specialty.setDoctorCount((int) count);
        specialtyRepository.save(specialty);
    }

    private Doctor findEntity(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
    }

    private DoctorResponse toResponse(Doctor d) {
        List<ScheduleBlockResponse> schedule = d.getSchedule() == null ? List.of() :
                d.getSchedule().stream()
                        .map(sb -> ScheduleBlockResponse.builder().day(sb.getDay()).from(sb.getFrom()).to(sb.getTo()).build())
                        .toList();

        return DoctorResponse.builder()
                .id(d.getId())
                .userId(d.getUser() != null ? d.getUser().getId() : null)
                .name(d.getName())
                .specialtyId(d.getSpecialty() != null ? d.getSpecialty().getId() : null)
                .specialtyName(d.getSpecialty() != null ? d.getSpecialty().getName() : null)
                .photo(d.getPhoto())
                .experience(d.getExperience())
                .rating(d.getRating())
                .education(d.getEducation())
                .certifications(d.getCertifications())
                .bio(d.getBio())
                .consultationTypes(d.getConsultationTypes())
                .locationId(d.getLocation() != null ? d.getLocation().getId() : null)
                .locationName(d.getLocation() != null ? d.getLocation().getName() : null)
                .professionalId(d.getProfessionalId())
                .phone(d.getPhone())
                .email(d.getEmail())
                .status(d.getStatus())
                .schedule(schedule)
                .blockedDates(d.getBlockedDates())
                .build();
    }
}

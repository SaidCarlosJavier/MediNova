package com.sistema.sistema_hospitalario.service.impl;

import com.sistema.sistema_hospitalario.dto.request.UserRequest;
import com.sistema.sistema_hospitalario.dto.response.UserResponse;
import com.sistema.sistema_hospitalario.entity.User;
import com.sistema.sistema_hospitalario.enums.AccountStatus;
import com.sistema.sistema_hospitalario.exception.DuplicateResourceException;
import com.sistema.sistema_hospitalario.exception.ResourceNotFoundException;
import com.sistema.sistema_hospitalario.repository.UserRepository;
import com.sistema.sistema_hospitalario.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo: " + request.getEmail());
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(request.getStatus() != null ? request.getStatus() : AccountStatus.ACTIVE)
                .build();

        return toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = findEntity(id);

        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo: " + request.getEmail());
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setRole(request.getRole());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = findEntity(id);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void touchLastActivity(Long id) {
        userRepository.findById(id).ifPresent(u -> {
            u.setLastActivity(LocalDateTime.now());
            userRepository.save(u);
        });
    }

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .registered(user.getRegistered())
                .lastActivity(user.getLastActivity())
                .patientId(user.getPatient() != null ? user.getPatient().getId() : null)
                .doctorId(user.getDoctor() != null ? user.getDoctor().getId() : null)
                .build();
    }
}

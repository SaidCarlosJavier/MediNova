package com.sistema.sistema_hospitalario.service.impl;

import com.sistema.sistema_hospitalario.dto.request.LoginRequest;
import com.sistema.sistema_hospitalario.dto.request.RegisterRequest;
import com.sistema.sistema_hospitalario.dto.response.AuthResponse;
import com.sistema.sistema_hospitalario.entity.User;
import com.sistema.sistema_hospitalario.enums.AccountStatus;
import com.sistema.sistema_hospitalario.enums.AuditModule;
import com.sistema.sistema_hospitalario.enums.AuditStatus;
import com.sistema.sistema_hospitalario.exception.DuplicateResourceException;
import com.sistema.sistema_hospitalario.repository.UserRepository;
import com.sistema.sistema_hospitalario.security.UserPrincipal;
import com.sistema.sistema_hospitalario.security.jwt.JwtService;
import com.sistema.sistema_hospitalario.service.AuditLogService;
import com.sistema.sistema_hospitalario.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario registrado con el correo: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(AccountStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        auditLogService.log(saved.getName(), "Registro de nuevo usuario", AuditModule.AUTHENTICATION, AuditStatus.SUCCESS);

        UserPrincipal principal = UserPrincipal.build(saved);
        String token = jwtService.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .userId(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .role(saved.getRole())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado tras autenticación"));
        user.setLastActivity(java.time.LocalDateTime.now());
        userRepository.save(user);

        auditLogService.log(principal.getName(), "Inicio de sesión", AuditModule.AUTHENTICATION, AuditStatus.SUCCESS);

        return AuthResponse.builder()
                .token(token)
                .userId(principal.getId())
                .name(principal.getName())
                .email(principal.getEmail())
                .role(com.sistema.sistema_hospitalario.enums.Role.valueOf(principal.getRole()))
                .build();
    }
}

package com.sistema.sistema_hospitalario.service;

import com.sistema.sistema_hospitalario.dto.request.LoginRequest;
import com.sistema.sistema_hospitalario.dto.request.RegisterRequest;
import com.sistema.sistema_hospitalario.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}

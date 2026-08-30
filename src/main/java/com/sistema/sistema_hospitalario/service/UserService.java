package com.sistema.sistema_hospitalario.service;

import com.sistema.sistema_hospitalario.dto.request.UserRequest;
import com.sistema.sistema_hospitalario.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse create(UserRequest request);
    UserResponse getById(Long id);
    List<UserResponse> getAll();
    UserResponse update(Long id, UserRequest request);
    void delete(Long id);
    void touchLastActivity(Long id);
}

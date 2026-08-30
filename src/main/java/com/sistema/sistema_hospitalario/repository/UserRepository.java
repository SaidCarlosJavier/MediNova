package com.sistema.sistema_hospitalario.repository;

import com.sistema.sistema_hospitalario.entity.User;
import com.sistema.sistema_hospitalario.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    java.util.List<User> findByRole(Role role);
}

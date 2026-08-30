package com.sistema.sistema_hospitalario.service;

import com.sistema.sistema_hospitalario.dto.request.NotificationRequest;
import com.sistema.sistema_hospitalario.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    NotificationResponse create(NotificationRequest request);
    List<NotificationResponse> getByUser(Long userId);
    List<NotificationResponse> getUnreadByUser(Long userId);
    NotificationResponse markAsRead(Long id);
    void markAllAsRead(Long userId);
    void delete(Long id);
}

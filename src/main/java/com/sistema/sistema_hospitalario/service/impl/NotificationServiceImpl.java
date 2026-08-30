package com.sistema.sistema_hospitalario.service.impl;

import com.sistema.sistema_hospitalario.dto.request.NotificationRequest;
import com.sistema.sistema_hospitalario.dto.response.NotificationResponse;
import com.sistema.sistema_hospitalario.entity.Notification;
import com.sistema.sistema_hospitalario.entity.User;
import com.sistema.sistema_hospitalario.exception.ResourceNotFoundException;
import com.sistema.sistema_hospitalario.repository.NotificationRepository;
import com.sistema.sistema_hospitalario.repository.UserRepository;
import com.sistema.sistema_hospitalario.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public NotificationResponse create(NotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.getUserId()));

        Notification notification = Notification.builder()
                .user(user)
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .read(false)
                .build();

        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public List<NotificationResponse> getByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByDateDesc(userId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<NotificationResponse> getUnreadByUser(Long userId) {
        return notificationRepository.findByUserIdAndReadFalse(userId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación", "id", id));
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación", "id", id));
        notificationRepository.delete(notification);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUser().getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .date(n.getDate())
                .read(n.isRead())
                .build();
    }
}

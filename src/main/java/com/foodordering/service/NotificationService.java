package com.foodordering.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodordering.model.Notification;
import com.foodordering.model.User;
import com.foodordering.repository.NotificationRepository;
import com.foodordering.repository.UserRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Notification notifyUser(User user, String message, String link) {
        Notification notification = new Notification(user, message, link);
        Notification saved = notificationRepository.save(notification);
        push(user.getId(), saved);
        return saved;
    }

    @Transactional
    public void notifyAdmins(String message, String link) {
        List<User> admins = userRepository.findAllByRoleName("ROLE_ADMIN");
        for (User admin : admins) {
            Notification saved = notificationRepository.save(new Notification(admin, message, link));
            push(admin.getId(), saved);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> recent(User user) {
        return notificationRepository.findTop10ByUserOrderByIdDesc(user);
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    @Transactional
    public void markAllRead(User user) {
        for (Notification n : notificationRepository.findTop10ByUserAndReadFalse(user)) {
            n.setRead(true);
        }
    }

    public SseEmitter subscribe(User user) {
        SseEmitter emitter = new SseEmitter(0L);
        List<SseEmitter> list = emitters.computeIfAbsent(user.getId(), k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        emitter.onCompletion(() -> list.remove(emitter));
        emitter.onTimeout(() -> list.remove(emitter));
        emitter.onError(e -> list.remove(emitter));
        send(emitter, "connected", Map.of("connected", true));
        return emitter;
    }

    private void push(Long userId, Notification notification) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        Map<String, Object> data = Map.of(
                "id", notification.getId(),
                "message", notification.getMessage(),
                "link", notification.getLink() == null ? "" : notification.getLink(),
                "time", notification.getCreatedAt().toLocalTime().toString(),
                "count", unreadCount(notification.getUser()));
        for (SseEmitter emitter : list) {
            send(emitter, "notification", data);
        }
    }

    private void send(SseEmitter emitter, String name, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(objectMapper.writeValueAsString(data)));
        } catch (IOException | IllegalStateException ignored) {
            emitter.complete();
        }
    }
}
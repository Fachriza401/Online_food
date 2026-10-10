package com.foodordering.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodordering.exception.BadRequestException;
import com.foodordering.model.Notification;
import com.foodordering.model.User;
import com.foodordering.repository.NotificationRepository;
import com.foodordering.repository.UserRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Timeout stream SSE dalam milidetik. Nilai 0 atau negatif diabaikan oleh container
     * sehingga {@link SseEmitter} akan jatuh ke timeout bawaan (30 detik pada Tomcat) dan
     * memicu {@code AsyncRequestTimeoutException} pada respons yang output stream-nya sudah
     * dipakai. Dipakai batas aman 1 jam; EventSource di sisi browser otomatis menyambung ulang.
     */
    private static final long SSE_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1);

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
        notificationRepository.markAllRead(user);
    }

    public SseEmitter subscribe(User user) {
        if (user == null) {
            throw new BadRequestException("Sesi tidak valid, silakan login kembali.");
        }
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        Long userId = user.getId();
        List<SseEmitter> list = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> remove(userId, emitter));
        send(emitter, "connected", Map.of("connected", true));
        return emitter;
    }

    /** Lepas emitter dari daftar dan buang entri map bila sudah tidak ada subscribers. */
    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            emitters.remove(userId, list);
        }
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
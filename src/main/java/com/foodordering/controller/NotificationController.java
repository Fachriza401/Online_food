package com.foodordering.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.foodordering.model.User;
import com.foodordering.security.CurrentUser;
import com.foodordering.service.NotificationService;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping(value = "/api/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        User user = CurrentUser.get();
        return notificationService.subscribe(user);
    }

    @GetMapping("/api/notifications/unread")
    @ResponseBody
    public Map<String, Object> unread() {
        User user = CurrentUser.get();
        return Map.of("count", notificationService.unreadCount(user));
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead() {
        notificationService.markAllRead(CurrentUser.get());
        return "redirect:/";
    }
}
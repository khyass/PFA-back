package com.example.candidateservice.controller;

import com.example.candidateservice.dto.NotificationDTO;
import com.example.candidateservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Notifications", description = "Gestion des notifications candidat : consultation, marquage comme lu")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Lister les notifications", description = "Retourne toutes les notifications du candidat connecté, triées par date décroissante.")
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(@AuthenticationPrincipal Jwt jwt) {
        String candidateId = jwt.getSubject();
        List<NotificationDTO> notifications = notificationService.getNotifications(candidateId);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Nombre de non-lues", description = "Retourne le nombre de notifications non lues.")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        String candidateId = jwt.getSubject();
        long count = notificationService.getUnreadCount(candidateId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @Operation(summary = "Marquer comme lue", description = "Marque une notification spécifique comme lue.")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "ID de la notification") @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String candidateId = jwt.getSubject();
        notificationService.markAsRead(id, candidateId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Tout marquer comme lu", description = "Marque toutes les notifications du candidat comme lues.")
    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        String candidateId = jwt.getSubject();
        notificationService.markAllAsRead(candidateId);
        return ResponseEntity.ok().build();
    }
}

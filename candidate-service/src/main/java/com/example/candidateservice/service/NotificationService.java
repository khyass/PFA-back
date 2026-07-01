package com.example.candidateservice.service;

import com.example.candidateservice.dto.NotificationDTO;
import com.example.candidateservice.entity.CandidatureStatus;
import com.example.candidateservice.entity.Notification;
import com.example.candidateservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing candidate notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Creates a notification when a candidature status changes.
     */
    public void notifyStatusChange(String candidateId, String jobTitle, String companyName,
                                   CandidatureStatus oldStatus, CandidatureStatus newStatus,
                                   String interviewDate) {
        String title = buildTitle(newStatus);
        String message = buildMessage(jobTitle, companyName, oldStatus, newStatus, interviewDate);

        Notification notification = Notification.builder()
                .candidateId(candidateId)
                .title(title)
                .message(message)
                .read(false)
                .build();

        notificationRepository.save(notification);
        log.info("Notification created for candidate {} - status change to {}", candidateId, newStatus);
    }

    /**
     * Gets all notifications for a candidate.
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotifications(String candidateId) {
        return notificationRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Gets unread notification count.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String candidateId) {
        return notificationRepository.countByCandidateIdAndReadFalse(candidateId);
    }

    /**
     * Marks a notification as read.
     */
    public void markAsRead(UUID notificationId, String candidateId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getCandidateId().equals(candidateId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    /**
     * Marks all notifications as read for a candidate.
     */
    public void markAllAsRead(String candidateId) {
        List<Notification> unread = notificationRepository
                .findByCandidateIdAndReadFalseOrderByCreatedAtDesc(candidateId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private String buildTitle(CandidatureStatus newStatus) {
        return switch (newStatus) {
            case REVIEWING -> "📋 Candidature en cours d'examen";
            case INTERVIEW -> "📅 Entretien planifié";
            case OFFER -> "🎉 Offre reçue";
            case HIRED -> "✅ Félicitations, vous êtes embauché !";
            case REJECTED -> "❌ Candidature refusée";
            case ACCEPTED -> "✅ Candidature acceptée";
            default -> "📢 Mise à jour de votre candidature";
        };
    }

    private String buildMessage(String jobTitle, String companyName,
                                CandidatureStatus oldStatus, CandidatureStatus newStatus,
                                String interviewDate) {
        String base = String.format("Votre candidature pour le poste \"%s\" chez %s", jobTitle, companyName);

        return switch (newStatus) {
            case REVIEWING -> base + " est maintenant en cours d'examen par le recruteur.";
            case INTERVIEW -> {
                String msg = base + " a été retenue pour un entretien.";
                if (interviewDate != null && !interviewDate.isBlank()) {
                    msg += " Date prévue : " + interviewDate + ".";
                }
                yield msg;
            }
            case OFFER -> base + " a abouti à une offre ! Consultez les détails.";
            case HIRED -> base + " est finalisée. Bienvenue dans l'équipe !";
            case REJECTED -> base + " n'a pas été retenue. Ne vous découragez pas, d'autres opportunités vous attendent.";
            case ACCEPTED -> base + " a été acceptée.";
            default -> base + " a été mise à jour.";
        };
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

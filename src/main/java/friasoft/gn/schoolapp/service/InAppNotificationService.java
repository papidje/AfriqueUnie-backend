package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.notification.MarkAllReadResponse;
import friasoft.gn.schoolapp.dto.notification.NotificationResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.auth.UserSchoolAffiliation;
import friasoft.gn.schoolapp.entity.notification.NotificationClosureReason;
import friasoft.gn.schoolapp.entity.notification.NotificationEntity;
import friasoft.gn.schoolapp.entity.notification.NotificationReadState;
import friasoft.gn.schoolapp.entity.notification.NotificationType;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.NotificationEntityRepository;
import friasoft.gn.schoolapp.repository.NotificationReadStateRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import friasoft.gn.schoolapp.repository.UserSchoolAffiliationRepository;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import friasoft.gn.schoolapp.tenancy.TenantHibernateFilterAspect;
import friasoft.gn.schoolapp.util.UserRoleFrenchLabel;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InAppNotificationService {

    private final NotificationEntityRepository notificationRepository;
    private final NotificationReadStateRepository readStateRepository;
    private final UserRepository userRepository;
    private final UserSchoolAffiliationRepository userSchoolAffiliationRepository;
    private final EntityManager entityManager;

    @Transactional
    public void createInvitationNotification(User invitee, UserSchoolAffiliation affiliation, String schoolDisplayName) {
        NotificationEntity n = new NotificationEntity();
        n.setTitle("Invitation à rejoindre un établissement");
        n.setContent(
            "« "
                + schoolDisplayName
                + " » vous invite avec le rôle "
                + UserRoleFrenchLabel.format(affiliation.getRole())
                + ". Acceptez pour activer votre rattachement."
        );
        n.setType(NotificationType.INVITATION);
        n.setCreatedAt(LocalDateTime.now());
        n.setLinkId(affiliation.getId());
        n.setTargetUser(invitee);
        notificationRepository.save(n);
    }

    /**
     * Message personnel (suspension, réactivation, etc.) — sans action Accepter / Refuser dans l’UI.
     */
    @Transactional
    public void createUserTargetedNotification(User targetUser, School school, String title, String content) {
        NotificationEntity n = new NotificationEntity();
        n.setTitle(title);
        n.setContent(content);
        n.setType(NotificationType.USER_TARGETED);
        n.setCreatedAt(LocalDateTime.now());
        n.setLinkId(null);
        n.setTargetUser(targetUser);
        n.setTargetSchool(school);
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(User user, boolean unreadOnly, Long tenantIdForTargeting) {
        return runWithoutTenantFilter(() -> {
            List<NotificationEntity> rows =
                notificationRepository.findListedForUser(user.getId(), tenantIdForTargeting);
            List<Long> ids = rows.stream().map(NotificationEntity::getId).toList();
            Map<Long, NotificationReadState> stateByNotificationId = new HashMap<>();
            if (!ids.isEmpty()) {
                for (NotificationReadState rs :
                    readStateRepository.findByUser_IdAndNotification_IdIn(user.getId(), ids)) {
                    stateByNotificationId.put(rs.getNotification().getId(), rs);
                }
            }
            Set<Long> readIds = stateByNotificationId.keySet();
            return rows.stream()
                .filter(n -> !unreadOnly || !readIds.contains(n.getId()))
                .map(n -> toResponse(n, stateByNotificationId.get(n.getId())))
                .toList();
        });
    }

    @Transactional(readOnly = true)
    public long countUnread(User user, Long tenantIdForTargeting) {
        return runWithoutTenantFilter(
            () -> notificationRepository.countUnreadForUser(user.getId(), tenantIdForTargeting));
    }

    /**
     * Upsert lecture pour toutes les notifications ciblées encore sans ligne {@code notification_read_states}.
     */
    @Transactional
    public MarkAllReadResponse markAllReadForUser(User user, Long tenantIdForTargeting) {
        return runWithoutTenantFilter(() -> {
            List<NotificationEntity> unread =
                notificationRepository.findUnreadTargetedForUser(user.getId(), tenantIdForTargeting);
            LocalDateTime now = LocalDateTime.now();
            int marked = 0;
            for (NotificationEntity n : unread) {
                Optional<NotificationReadState> opt =
                    readStateRepository.findByNotification_IdAndUser_Id(n.getId(), user.getId());
                if (opt.isPresent()) {
                    continue;
                }
                NotificationReadState rs = new NotificationReadState();
                rs.setNotification(n);
                rs.setUser(userRepository.getReferenceById(user.getId()));
                rs.setReadAt(now);
                rs.setProcessed(false);
                rs.setUpdatedAt(now);
                rs.setVisible(true);
                rs.setClosureReason(NotificationClosureReason.MARK_READ);
                readStateRepository.save(rs);
                marked++;
            }
            return new MarkAllReadResponse(marked);
        });
    }

    /**
     * Masque la notification pour l’utilisateur (bouton fermer). Crée un état « lu » si besoin pour sortir du compteur non lu.
     */
    @Transactional
    public void dismissForUser(User user, Long notificationId, Long tenantIdForTargeting) {
        runWithoutTenantFilter(() -> {
            if (notificationRepository.countAccessibleByUser(notificationId, user.getId(), tenantIdForTargeting) == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable.");
            }
            LocalDateTime now = LocalDateTime.now();
            NotificationEntity ref = notificationRepository.getReferenceById(notificationId);
            Optional<NotificationReadState> opt =
                readStateRepository.findByNotification_IdAndUser_Id(notificationId, user.getId());
            if (opt.isPresent()) {
                NotificationReadState r = opt.get();
                if (!r.isVisible()) {
                    return null;
                }
                r.setVisible(false);
                r.setUpdatedAt(now);
                r.setClosureReason(NotificationClosureReason.DISMISSED);
                readStateRepository.save(r);
                return null;
            }
            NotificationReadState rs = new NotificationReadState();
            rs.setNotification(ref);
            rs.setUser(userRepository.getReferenceById(user.getId()));
            rs.setReadAt(now);
            rs.setProcessed(false);
            rs.setUpdatedAt(now);
            rs.setVisible(false);
            rs.setClosureReason(NotificationClosureReason.DISMISSED);
            readStateRepository.save(rs);
            return null;
        });
    }

    @Transactional
    public void markInvitationNotificationsRead(
        Long userId,
        Long affiliationLinkId,
        NotificationClosureReason invitationOutcome
    ) {
        runWithoutTenantFilter(() -> {
            boolean processed =
                invitationOutcome == NotificationClosureReason.INVITATION_ACCEPTED
                    || invitationOutcome == NotificationClosureReason.INVITATION_REFUSED;
            List<NotificationEntity> list =
                notificationRepository.findByLinkIdAndTargetUser_IdAndType(
                    affiliationLinkId,
                    userId,
                    NotificationType.INVITATION
                );
            for (NotificationEntity n : list) {
                ensureReadState(n, userId, processed, invitationOutcome);
            }
            return null;
        });
    }

    /**
     * Les entités liées ({@code User}, {@code UserSchoolAffiliation}, écoles…) portent le filtre Hibernate tenant.
     * Pour un utilisateur sans visibilité tenant (toutes affiliations inactives), ce filtre masquerait ses lignes de
     * notifications pourtant ciblées par {@code target_user_id}.
     */
    private <T> T runWithoutTenantFilter(java.util.concurrent.Callable<T> action) {
        Session session = entityManager.unwrap(Session.class);
        boolean hadFilter = session.getEnabledFilter(TenantHibernateFilterAspect.TENANT_FILTER_NAME) != null;
        Long tenantParam = TenantContext.getTenantId();
        if (hadFilter) {
            session.disableFilter(TenantHibernateFilterAspect.TENANT_FILTER_NAME);
        }
        try {
            return action.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            if (hadFilter && tenantParam != null) {
                Filter restored = session.enableFilter(TenantHibernateFilterAspect.TENANT_FILTER_NAME);
                restored.setParameter(TenantHibernateFilterAspect.TENANT_FILTER_PARAM, tenantParam);
            }
        }
    }

    private void ensureReadState(
        NotificationEntity n,
        Long userId,
        boolean processed,
        NotificationClosureReason closureReason
    ) {
        LocalDateTime now = LocalDateTime.now();
        Optional<NotificationReadState> opt = readStateRepository.findByNotification_IdAndUser_Id(n.getId(), userId);
        if (opt.isPresent()) {
            NotificationReadState r = opt.get();
            r.setProcessed(processed);
            r.setUpdatedAt(now);
            r.setVisible(true);
            if (closureReason != null) {
                r.setClosureReason(closureReason);
            }
            readStateRepository.save(r);
            return;
        }
        NotificationReadState rs = new NotificationReadState();
        rs.setNotification(n);
        rs.setUser(userRepository.getReferenceById(userId));
        rs.setReadAt(now);
        rs.setProcessed(processed);
        rs.setUpdatedAt(now);
        rs.setVisible(true);
        rs.setClosureReason(closureReason);
        readStateRepository.save(rs);
    }

    private NotificationResponse toResponse(NotificationEntity n, NotificationReadState state) {
        boolean read = state != null;
        boolean processed = state != null && state.isProcessed();
        LocalDateTime updatedAt = state != null ? state.getUpdatedAt() : null;
        boolean visible = state == null || state.isVisible();
        String closureReason = state != null && state.getClosureReason() != null
            ? state.getClosureReason().name()
            : null;
        String schoolName = null;
        if (n.getTargetSchool() != null) {
            schoolName = schoolDisplayName(n.getTargetSchool());
        } else if (n.getType() == NotificationType.INVITATION && n.getLinkId() != null) {
            schoolName = userSchoolAffiliationRepository
                .findById(n.getLinkId())
                .map(UserSchoolAffiliation::getSchool)
                .map(InAppNotificationService::schoolDisplayName)
                .orElse(null);
        }
        return new NotificationResponse(
            n.getId(),
            n.getTitle(),
            n.getContent(),
            n.getType().name(),
            n.getCreatedAt(),
            n.getLinkId(),
            read,
            processed,
            updatedAt,
            visible,
            closureReason,
            schoolName
        );
    }

    private static String schoolDisplayName(School school) {
        if (school == null) {
            return null;
        }
        if (school.getName() != null && !school.getName().isBlank()) {
            return school.getName().trim();
        }
        return school.getId() != null ? ("Établissement #" + school.getId()) : "Établissement";
    }
}

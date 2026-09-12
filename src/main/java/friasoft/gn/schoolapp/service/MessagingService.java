package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.messaging.MessagingConversationDto;
import friasoft.gn.schoolapp.dto.messaging.MessagingMessageDto;
import friasoft.gn.schoolapp.dto.messaging.MessagingUnreadSummaryDto;
import friasoft.gn.schoolapp.dto.messaging.MessagingUserSummaryDto;
import friasoft.gn.schoolapp.dto.messaging.SendMessageRequest;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.auth.UserPlatformRole;
import friasoft.gn.schoolapp.entity.messaging.MessagingConversation;
import friasoft.gn.schoolapp.entity.messaging.MessagingMessage;
import friasoft.gn.schoolapp.entity.messaging.MessagingParticipant;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.UserPlatformRoleRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import friasoft.gn.schoolapp.repository.UserSchoolAffiliationRepository;
import friasoft.gn.schoolapp.repository.messaging.MessagingConversationRepository;
import friasoft.gn.schoolapp.repository.messaging.MessagingMessageRepository;
import friasoft.gn.schoolapp.repository.messaging.MessagingParticipantRepository;
import friasoft.gn.schoolapp.util.UserRoleFrenchLabel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private static final int DEFAULT_MESSAGE_LIMIT = 50;
    private static final int MAX_MESSAGE_LIMIT = 100;
    private static final int PREVIEW_MAX = 280;

    private final MessagingConversationRepository conversationRepository;
    private final MessagingParticipantRepository participantRepository;
    private final MessagingMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final UserSchoolAffiliationRepository affiliationRepository;

    @Transactional(readOnly = true)
    public MessagingUnreadSummaryDto unreadSummary(User principal) {
        return new MessagingUnreadSummaryDto(
            participantRepository.countConversationsWithUnread(principal.getId())
        );
    }

    @Transactional(readOnly = true)
    public List<MessagingConversationDto> listConversations(User principal, String q) {
        String term = normalizeQuery(q);
        List<MessagingConversation> conversations =
            conversationRepository.findForUserFiltered(principal.getId(), term);
        List<MessagingConversationDto> rows = new ArrayList<>(conversations.size());
        for (MessagingConversation c : conversations) {
            rows.add(toConversationDto(c, principal.getId()));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<MessagingUserSummaryDto> listContacts(User principal, String q) {
        String term = normalizeQuery(q);
        User.UserRole myPlatform = platformRole(principal);
        Map<Long, User> byId = new LinkedHashMap<>();

        if (myPlatform == User.UserRole.SUPER_ADMIN) {
            for (User admin : userRepository.findAllOrganizationAdmins()) {
                if (Objects.equals(admin.getId(), principal.getId())) {
                    continue;
                }
                if (!admin.isActive()) {
                    continue;
                }
                if (!matchesQuery(admin, term)) {
                    continue;
                }
                byId.put(admin.getId(), admin);
            }
        } else if (myPlatform == User.UserRole.ADMIN_ECOLE) {
            Long tenantId = principal.getOrganizationTenantId();
            if (tenantId != null) {
                for (User u : affiliationRepository.findActiveAffiliatedUsersByTenant(
                    tenantId, principal.getId(), term
                )) {
                    byId.put(u.getId(), u);
                }
                for (User admin : userRepository.findAllOrganizationAdmins()) {
                    if (Objects.equals(admin.getId(), principal.getId())) {
                        continue;
                    }
                    if (!admin.isActive()) {
                        continue;
                    }
                    if (!Objects.equals(admin.getOrganizationTenantId(), tenantId)) {
                        continue;
                    }
                    if (!matchesQuery(admin, term)) {
                        continue;
                    }
                    byId.put(admin.getId(), admin);
                }
            }
            for (User u : userPlatformRoleRepository.findActiveUsersByRole(User.UserRole.SUPER_ADMIN)) {
                if (Objects.equals(u.getId(), principal.getId())) {
                    continue;
                }
                if (!matchesQuery(u, term)) {
                    continue;
                }
                byId.put(u.getId(), u);
            }
        } else {
            List<School> schools = affiliationRepository.findActiveSchoolsForUser(principal.getId());
            List<Long> schoolIds = schools.stream().map(School::getId).toList();
            if (!schoolIds.isEmpty()) {
                for (User u : affiliationRepository.findActiveAffiliatedUsersBySchoolIds(
                    schoolIds, principal.getId(), term
                )) {
                    byId.put(u.getId(), u);
                }
            }
            Long tenantId = resolveOrgTenantFromAffiliations(principal);
            if (tenantId != null) {
                for (User admin : userRepository.findAllOrganizationAdmins()) {
                    if (!admin.isActive()) {
                        continue;
                    }
                    if (!Objects.equals(admin.getOrganizationTenantId(), tenantId)) {
                        continue;
                    }
                    if (Objects.equals(admin.getId(), principal.getId())) {
                        continue;
                    }
                    if (!matchesQuery(admin, term)) {
                        continue;
                    }
                    byId.put(admin.getId(), admin);
                }
            }
        }

        return byId.values().stream()
            .sorted(Comparator.comparing(this::displayName, String.CASE_INSENSITIVE_ORDER))
            .map(this::toUserSummary)
            .toList();
    }

    @Transactional
    public MessagingConversationDto getOrCreateWith(User principal, Long otherUserId) {
        if (otherUserId == null || Objects.equals(otherUserId, principal.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destinataire invalide.");
        }
        User other = userRepository.findById(otherUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        if (!other.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce compte n’est pas actif.");
        }
        assertCanMessage(principal, other);

        Optional<MessagingConversation> existing =
            conversationRepository.findOneToOneBetween(principal.getId(), other.getId());
        if (existing.isPresent()) {
            return toConversationDto(existing.get(), principal.getId());
        }

        MessagingConversation conversation = conversationRepository.save(new MessagingConversation());
        MessagingParticipant p1 = new MessagingParticipant();
        p1.setConversation(conversation);
        p1.setUser(principal);
        p1.setLastReadAt(Instant.now());
        MessagingParticipant p2 = new MessagingParticipant();
        p2.setConversation(conversation);
        p2.setUser(other);
        participantRepository.save(p1);
        participantRepository.save(p2);
        return toConversationDto(conversation, principal.getId());
    }

    @Transactional(readOnly = true)
    public List<MessagingMessageDto> listMessages(
        User principal,
        Long conversationId,
        Long afterId,
        Long beforeId,
        Integer limit
    ) {
        requireParticipant(conversationId, principal.getId());
        int size = limit == null ? DEFAULT_MESSAGE_LIMIT : Math.min(Math.max(limit, 1), MAX_MESSAGE_LIMIT);

        List<MessagingMessage> rows;
        if (afterId != null) {
            rows = messageRepository.findPageAscending(
                conversationId, afterId, null, PageRequest.of(0, size)
            );
        } else if (beforeId != null) {
            List<MessagingMessage> newestFirst = messageRepository.findNewestBefore(
                conversationId, beforeId, PageRequest.of(0, size)
            );
            rows = newestFirst.stream()
                .sorted(Comparator.comparing(MessagingMessage::getId))
                .toList();
        } else {
            List<MessagingMessage> newestFirst = messageRepository.findNewestBefore(
                conversationId, null, PageRequest.of(0, size)
            );
            rows = newestFirst.stream()
                .sorted(Comparator.comparing(MessagingMessage::getId))
                .toList();
        }
        return rows.stream().map(m -> toMessageDto(m, principal.getId())).toList();
    }

    @Transactional
    public MessagingMessageDto sendMessage(User principal, Long conversationId, SendMessageRequest request) {
        MessagingParticipant me = requireParticipant(conversationId, principal.getId());
        User other = counterpart(conversationId, principal.getId());
        assertCanMessage(principal, other);

        String body = request.body() == null ? "" : request.body().trim();
        if (body.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message vide.");
        }
        if (body.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message trop long (max 4000).");
        }

        MessagingConversation conversation = me.getConversation();
        MessagingMessage message = new MessagingMessage();
        message.setConversation(conversation);
        message.setSender(principal);
        message.setBody(body);
        message = messageRepository.save(message);

        conversation.setLastMessageAt(message.getCreatedAt() != null ? message.getCreatedAt() : Instant.now());
        conversation.setLastMessagePreview(preview(body));
        conversationRepository.save(conversation);

        me.setLastReadAt(Instant.now());
        participantRepository.save(me);

        return toMessageDto(message, principal.getId());
    }

    @Transactional
    public void markRead(User principal, Long conversationId) {
        MessagingParticipant me = requireParticipant(conversationId, principal.getId());
        me.setLastReadAt(Instant.now());
        participantRepository.save(me);
    }

    private void assertCanMessage(User a, User b) {
        if (Objects.equals(a.getId(), b.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destinataire invalide.");
        }
        User.UserRole roleA = platformRole(a);
        User.UserRole roleB = platformRole(b);

        if (roleA == User.UserRole.SUPER_ADMIN && roleB == User.UserRole.ADMIN_ECOLE) {
            return;
        }
        if (roleB == User.UserRole.SUPER_ADMIN && roleA == User.UserRole.ADMIN_ECOLE) {
            return;
        }
        if (roleA == User.UserRole.ADMIN_ECOLE && roleB == User.UserRole.ADMIN_ECOLE) {
            Long tA = a.getOrganizationTenantId();
            Long tB = b.getOrganizationTenantId();
            if (tA != null && tA.equals(tB)) {
                return;
            }
        }
        if (roleA == User.UserRole.ADMIN_ECOLE) {
            Long tenantId = a.getOrganizationTenantId();
            if (tenantId != null && affiliationRepository.hasActiveAffiliationInTenant(b.getId(), tenantId)) {
                return;
            }
        }
        if (roleB == User.UserRole.ADMIN_ECOLE) {
            Long tenantId = b.getOrganizationTenantId();
            if (tenantId != null && affiliationRepository.hasActiveAffiliationInTenant(a.getId(), tenantId)) {
                return;
            }
        }
        if (affiliationRepository.shareActiveSchool(a.getId(), b.getId())) {
            return;
        }
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Vous ne pouvez pas contacter cet utilisateur."
        );
    }

    private MessagingParticipant requireParticipant(Long conversationId, Long userId) {
        return participantRepository.findByConversation_IdAndUser_Id(conversationId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversation inaccessible."));
    }

    private User counterpart(Long conversationId, Long userId) {
        return participantRepository.findByConversation_Id(conversationId).stream()
            .map(MessagingParticipant::getUser)
            .filter(u -> !Objects.equals(u.getId(), userId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Correspondant introuvable."));
    }

    private MessagingConversationDto toConversationDto(MessagingConversation c, Long myId) {
        User other = counterpart(c.getId(), myId);
        long unread = participantRepository.countUnreadInConversation(myId, c.getId());
        return new MessagingConversationDto(
            c.getId(),
            toUserSummary(other),
            c.getLastMessagePreview(),
            c.getLastMessageAt(),
            unread
        );
    }

    private MessagingMessageDto toMessageDto(MessagingMessage m, Long myId) {
        return new MessagingMessageDto(
            m.getId(),
            m.getConversation().getId(),
            m.getSender().getId(),
            m.getBody(),
            m.getCreatedAt(),
            Objects.equals(m.getSender().getId(), myId)
        );
    }

    private MessagingUserSummaryDto toUserSummary(User u) {
        User.UserRole platform = platformRole(u);
        String roleLabel = platform != null
            ? UserRoleFrenchLabel.format(platform)
            : "Utilisateur";
        if (platform == null) {
            List<School> schools = affiliationRepository.findActiveSchoolsForUser(u.getId());
            if (!schools.isEmpty()) {
                var affs = affiliationRepository.findAllByUser_IdAndSchool_IdAndActiveTrue(
                    u.getId(), schools.get(0).getId()
                );
                if (!affs.isEmpty()) {
                    roleLabel = UserRoleFrenchLabel.format(affs.get(0).getRole());
                }
            }
        }
        return new MessagingUserSummaryDto(u.getId(), displayName(u), u.getEmail(), roleLabel);
    }

    private User.UserRole platformRole(User user) {
        return userPlatformRoleRepository.findByUser_Id(user.getId())
            .map(UserPlatformRole::getRole)
            .orElse(null);
    }

    private Long resolveOrgTenantFromAffiliations(User user) {
        List<School> schools = affiliationRepository.findActiveSchoolsForUser(user.getId());
        return schools.stream()
            .map(School::getTenantId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(user.getOrganizationTenantId());
    }

    private static String normalizeQuery(String q) {
        if (q == null) {
            return "";
        }
        return q.trim();
    }

    private boolean matchesQuery(User u, String term) {
        if (term == null || term.isEmpty()) {
            return true;
        }
        String t = term.toLowerCase(Locale.ROOT);
        String name = displayName(u).toLowerCase(Locale.ROOT);
        String email = u.getEmail() != null ? u.getEmail().toLowerCase(Locale.ROOT) : "";
        return name.contains(t) || email.contains(t);
    }

    private String displayName(User u) {
        if (u.getFullname() != null && !u.getFullname().isBlank()) {
            return u.getFullname().trim();
        }
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String ln = u.getLastName() != null ? u.getLastName().trim() : "";
        String composed = (fn + " " + ln).trim();
        if (!composed.isEmpty()) {
            return composed;
        }
        return u.getEmail() != null ? u.getEmail() : "Utilisateur";
    }

    private static String preview(String body) {
        String oneLine = body.replace('\n', ' ').trim();
        if (oneLine.length() <= PREVIEW_MAX) {
            return oneLine;
        }
        return oneLine.substring(0, PREVIEW_MAX - 1) + "…";
    }
}

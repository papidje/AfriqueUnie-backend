package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ActivationRequest;
import friasoft.gn.schoolapp.dto.InviteUserDTO;
import friasoft.gn.schoolapp.dto.InviteUserResponse;
import friasoft.gn.schoolapp.dto.OwnProfileUpdateRequest;
import friasoft.gn.schoolapp.dto.SchoolRoleAssignmentDTO;
import friasoft.gn.schoolapp.dto.UpdateUserAffiliationsRequest;
import friasoft.gn.schoolapp.dto.UserAffiliationResponse;
import friasoft.gn.schoolapp.entity.auth.Activation;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.auth.UserPlatformRole;
import friasoft.gn.schoolapp.entity.auth.UserSchoolAffiliation;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.IActivationRepository;
import friasoft.gn.schoolapp.repository.UserPlatformRoleRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import friasoft.gn.schoolapp.repository.UserSchoolAffiliationRepository;
import friasoft.gn.schoolapp.service.communication.CommunicationMailDispatchService;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import friasoft.gn.schoolapp.tenancy.TenantHibernateFilterAspect;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import org.springframework.dao.DataIntegrityViolationException;

import org.hibernate.Session;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class UserService implements UserDetailsService{

    private final UserRepository userRepository;
    private final IActivationRepository iActivationRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final InAppNotificationService inAppNotificationService;
    private final CommunicationMailDispatchService communicationMailDispatchService;
    private final SchoolService schoolService;
    private final UserSchoolAffiliationRepository userSchoolAffiliationRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final UserCapabilityService userCapabilityService;
    private final EntityManager entityManager;

    /** Crée une activation, envoie le mail et retourne l’enregistrement (ex. code pour l’API d’invitation). */
    public Activation sendActivationEmail(User user) {
        Activation activation = createActivation(user);
        this.notificationService.sendActivationMail(activation);
        return activation;
    }

    /**
     * Remplace toute activation existante pour l’utilisateur, en enregistre une nouvelle et envoie le mail.
     * Réservé aux comptes encore inactifs (invitation non finalisée).
     */
    @Transactional
    @PreAuthorize("@userMgmtSecurity.canManageDirectory(authentication)")
    public void resendActivationEmail(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant utilisateur obligatoire.");
        }
        User current = getUserInfo();
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User target = this.userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        assertCurrentUserMayManageTargetUser(current, target);
        if (target.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce compte est déjà activé.");
        }
        if (target.getEmail() == null || target.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune adresse e-mail pour cet utilisateur.");
        }
        sendActivationEmail(target);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@userMgmtSecurity.canManageDirectory(authentication)")
    public List<User> getAll() {
        User current = getUserInfo();
        if (current == null) {
            return List.of();
        }
        if (userCapabilityService.isDirectorPrimarySchool(current)) {
            School dirSchool = current.getSchool();
            if (dirSchool == null || dirSchool.getId() == null) {
                return List.of();
            }
            return this.userRepository.findDistinctUsersWithActiveTeacherStaffAtSchool(dirSchool.getId());
        }
        // Le filtre Hibernate sur User inclut déjà les comptes rattachés au tenant (dont invitations cross-tenant en attente).
        // Ne pas ré-filter sur users.tenant_id : sinon les invités venant d’un autre tenant disparaissent de l’annuaire.
        return this.userRepository.findAll().stream()
            .filter(u -> !userCapabilityService.isSuperAdmin(u))
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<UserAffiliationResponse>> getActiveAffiliationSummariesGrouped(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserSchoolAffiliation> rows =
            this.userSchoolAffiliationRepository.findAllActiveWithSchoolByUser_IdIn(userIds);
        Map<Long, List<UserAffiliationResponse>> map = new LinkedHashMap<>();
        for (UserSchoolAffiliation a : rows) {
            map.computeIfAbsent(a.getUser().getId(), k -> new ArrayList<>()).add(toAffiliationSummary(a));
        }
        return map;
    }

    /**
     * Affiliations visibles dans l’annuaire pour un tenant (actives ou invitation en attente).
     */
    @Transactional(readOnly = true)
    public Map<Long, List<UserAffiliationResponse>> getDirectoryAffiliationSummariesGrouped(
        Collection<Long> userIds,
        Long tenantId
    ) {
        if (tenantId == null || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserSchoolAffiliation> rows =
            userSchoolAffiliationRepository.findAllWithSchoolByUser_IdInAndTenantId(userIds, tenantId);
        Map<Long, List<UserAffiliationResponse>> map = new LinkedHashMap<>();
        for (UserSchoolAffiliation a : rows) {
            map.computeIfAbsent(a.getUser().getId(), k -> new ArrayList<>()).add(toAffiliationSummary(a));
        }
        return map;
    }

    private static UserAffiliationResponse toAffiliationSummary(UserSchoolAffiliation a) {
        School s = a.getSchool();
        String name = (s.getName() != null && !s.getName().isBlank())
            ? s.getName().trim()
            : ("Établissement #" + s.getId());
        boolean invitationPending = !a.isActive() && !a.isShowInfoToTenant();
        boolean reactivationEligible = !a.isActive() && a.isShowInfoToTenant();
        return new UserAffiliationResponse(
            s.getId(),
            name,
            a.getRole().name(),
            a.isActive(),
            invitationPending,
            reactivationEligible
        );
    }

    @Transactional(readOnly = true)
    public Long resolveDirectoryTenantId(User viewer) {
        if (viewer == null) {
            return null;
        }
        if (userCapabilityService.isDirectorPrimarySchool(viewer)) {
            School ds = viewer.getSchool();
            return ds != null ? ds.getTenantId() : null;
        }
        return viewer.getOrganizationTenantId() != null ? viewer.getOrganizationTenantId() : viewer.getTenantId();
    }

    private static final class InvitePlan {
        final User.UserRole accountRole;
        final School primarySchool;
        final Long tenantId;
        final List<SchoolRoleAssignmentDTO> assignments;

        InvitePlan(
            User.UserRole accountRole,
            School primarySchool,
            Long tenantId,
            List<SchoolRoleAssignmentDTO> assignments
        ) {
            this.accountRole = accountRole;
            this.primarySchool = primarySchool;
            this.tenantId = tenantId;
            this.assignments = assignments;
        }
    }

    @Transactional
    @PreAuthorize("@userMgmtSecurity.canManageDirectory(authentication)")
    public InviteUserResponse inviteUser(InviteUserDTO request) {
        User current = getUserInfo();
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        if (request.email() == null || !request.email().contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse e-mail invalide.");
        }
        User.UserRole invitedRole = request.role();
        if (invitedRole == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle obligatoire.");
        }
        String emailNorm = request.email().trim().toLowerCase(java.util.Locale.ROOT);

        Long directorSchoolId = null;
        if (userCapabilityService.isDirectorPrimarySchool(current)) {
            School ds = current.getSchool();
            if (ds != null && ds.getId() != null) {
                directorSchoolId = ds.getId();
            }
        }

        List<SchoolRoleAssignmentDTO> normalized = normalizeSchoolAssignments(request, directorSchoolId);

        Optional<User> existingOpt = findUserByEmailForInviteUnscoped(emailNorm);
        if (existingOpt.isPresent()) {
            return inviteExistingUser(current, existingOpt.get(), invitedRole, directorSchoolId, normalized);
        }

        return inviteNewUser(current, emailNorm, invitedRole, directorSchoolId, normalized);
    }

    /**
     * Recherche par e-mail hors filtre tenant Hibernate : des comptes avec uniquement des affiliations
     * peuvent avoir {@code users.tenant_id} et {@code users.school_id} null ; avec le filtre ils sont invisibles
     * et l’invitation retombait par erreur sur {@link #inviteNewUser} (nouveau mot de passe + mail d’activation).
     */
    private Optional<User> findUserByEmailForInviteUnscoped(String emailNorm) {
        Session session = entityManager.unwrap(Session.class);
        boolean hadTenantFilter = session.getEnabledFilter(TenantHibernateFilterAspect.TENANT_FILTER_NAME) != null;
        if (hadTenantFilter) {
            session.disableFilter(TenantHibernateFilterAspect.TENANT_FILTER_NAME);
        }
        try {
            return userRepository.findByEmailIgnoreCase(emailNorm);
        } finally {
            if (hadTenantFilter) {
                Long tid = TenantContext.getTenantId();
                if (tid != null) {
                    session.enableFilter(TenantHibernateFilterAspect.TENANT_FILTER_NAME)
                        .setParameter(TenantHibernateFilterAspect.TENANT_FILTER_PARAM, tid);
                }
            }
        }
    }

    private static boolean isLikelyDuplicateUserEmail(DataIntegrityViolationException ex) {
        Throwable root = ex.getMostSpecificCause();
        String msg = root != null ? root.getMessage() : ex.getMessage();
        if (msg == null) {
            return false;
        }
        String m = msg.toLowerCase(java.util.Locale.ROOT);
        return m.contains("uk_users_email_lower")
            || (m.contains("duplicate key") && m.contains("users") && m.contains("email"));
    }

    private InvitePlan buildInvitePlan(
        User current,
        User.UserRole invitedRole,
        Long directorSchoolId,
        List<SchoolRoleAssignmentDTO> normalized
    ) {
        User.UserRole currentRole =
            userCapabilityService.isAdminEcole(current)
                ? User.UserRole.ADMIN_ECOLE
                : userCapabilityService.isDirectorPrimarySchool(current)
                    ? User.UserRole.DIRECTOR
                    : null;
        if (currentRole == User.UserRole.DIRECTOR) {
            if (invitedRole != User.UserRole.TEACHER && invitedRole != User.UserRole.STAFF) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle non autorisé pour un directeur.");
            }
            if (directorSchoolId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "École du directeur introuvable.");
            }
            schoolService.assertCurrentUserCanAccessSchool(directorSchoolId);
            School dirSchool = current.getSchool();
            List<SchoolRoleAssignmentDTO> assignments =
                List.of(new SchoolRoleAssignmentDTO(directorSchoolId, invitedRole));
            return new InvitePlan(invitedRole, dirSchool, dirSchool.getTenantId(), assignments);
        }
        if (currentRole == User.UserRole.ADMIN_ECOLE) {
            Long orgTenant = current.getOrganizationTenantId();
            if (orgTenant == null && current.getTenantId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contexte établissement introuvable.");
            }
            if (invitedRole == User.UserRole.ADMIN_ECOLE) {
                if (!normalized.isEmpty()) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "schoolAssignments ne doit pas être fourni pour un ADMIN_ECOLE."
                    );
                }
                Long tenant = orgTenant != null ? orgTenant : current.getTenantId();
                return new InvitePlan(User.UserRole.ADMIN_ECOLE, null, tenant, List.of());
            }
            if (invitedRole == User.UserRole.DIRECTOR) {
                if (normalized.size() != 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une école obligatoire pour un directeur.");
                }
                SchoolRoleAssignmentDTO a = normalized.get(0);
                if (a.role() != User.UserRole.DIRECTOR) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Rôle directeur attendu pour l'établissement."
                    );
                }
                schoolService.assertCurrentUserCanAccessSchool(a.schoolId());
                School linked = schoolService.getSchool(a.schoolId());
                return new InvitePlan(
                    User.UserRole.DIRECTOR,
                    linked,
                    linked.getTenantId(),
                    List.of(new SchoolRoleAssignmentDTO(linked.getId(), User.UserRole.DIRECTOR))
                );
            }
            if (invitedRole == User.UserRole.STAFF || invitedRole == User.UserRole.TEACHER) {
                if (normalized.isEmpty()) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Au moins une affiliation (école + rôle) est requise."
                    );
                }
                for (SchoolRoleAssignmentDTO a : normalized) {
                    if (a.role() != User.UserRole.STAFF && a.role() != User.UserRole.TEACHER) {
                        throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Chaque affiliation doit être STAFF ou TEACHER."
                        );
                    }
                    schoolService.assertCurrentUserCanAccessSchool(a.schoolId());
                }
                SchoolRoleAssignmentDTO firstA = normalized.get(0);
                User.UserRole accountRole = firstA.role();
                School first = schoolService.getSchool(firstA.schoolId());
                Long tenant = orgTenant != null ? orgTenant : current.getTenantId();
                Long resolvedTenant = tenant != null ? tenant : first.getTenantId();
                return new InvitePlan(accountRole, first, resolvedTenant, normalized);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle non autorisé.");
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
    }

    private void assertTenantCompatibleForExistingInvite(User inviter, User target, Long directorSchoolId) {
        if (userCapabilityService.isAdminEcole(inviter)) {
            return;
        }
        if (userCapabilityService.isDirectorPrimarySchool(inviter)) {
            School ds = inviter.getSchool();
            if (ds == null || ds.getId() == null || directorSchoolId == null || !directorSchoolId.equals(ds.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "École du directeur introuvable.");
            }
            return;
        }
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Vous n’avez pas les droits nécessaires pour cette invitation."
        );
    }

    private boolean shouldRecordCrossTenantPendingInvite(User inviter, User target) {
        if (userCapabilityService.isSuperAdmin(target) || userCapabilityService.isAdminEcole(target)) {
            return false;
        }
        Long invTenant = resolveInviterTenantId(inviter);
        Long tgtTenant = resolveTenantIdForInviteChecks(target);
        if (invTenant == null || tgtTenant == null) {
            return false;
        }
        return !invTenant.equals(tgtTenant);
    }

    private Long resolveInviterTenantId(User inviter) {
        if (userCapabilityService.isAdminEcole(inviter)) {
            Long t = inviter.getOrganizationTenantId();
            return t != null ? t : inviter.getTenantId();
        }
        if (inviter.getSchool() != null) {
            return inviter.getSchool().getTenantId();
        }
        return TenantContext.getTenantId();
    }

    private List<UserSchoolAffiliation> createPendingCrossTenantAffiliations(
        User target,
        List<SchoolRoleAssignmentDTO> assignments
    ) {
        List<UserSchoolAffiliation> created = new ArrayList<>();
        for (SchoolRoleAssignmentDTO a : assignments) {
            School school = schoolService.getSchool(a.schoolId());
            Optional<UserSchoolAffiliation> existing =
                this.userSchoolAffiliationRepository.findByUser_IdAndSchool_IdAndRole(
                    target.getId(), school.getId(), a.role());
            if (existing.isPresent()) {
                continue;
            }
            UserSchoolAffiliation aff = new UserSchoolAffiliation();
            aff.setUser(target);
            aff.setSchool(school);
            aff.setRole(a.role());
            aff.setActive(false);
            aff.setShowInfoToTenant(false);
            UserSchoolAffiliation saved = this.userSchoolAffiliationRepository.save(aff);
            created.add(saved);
        }
        return created;
    }

    /**
     * Tenant « effectif » pour les contrôles d’invitation : colonne / école principale, sinon première affiliation active.
     */
    private Long resolveTenantIdForInviteChecks(User user) {
        Long tid = user.getTenantId();
        if (tid != null) {
            return tid;
        }
        if (user.getId() == null) {
            return null;
        }
        List<UserSchoolAffiliation> rows =
            this.userSchoolAffiliationRepository.findAllActiveWithSchoolByUser_IdIn(Set.of(user.getId()));
        return rows.stream()
            .map(UserSchoolAffiliation::getSchool)
            .filter(Objects::nonNull)
            .map(School::getTenantId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private void assertActiveAffiliationDuplicates(User target, List<SchoolRoleAssignmentDTO> assignments) {
        if (target.getId() == null) {
            return;
        }
        for (SchoolRoleAssignmentDTO a : assignments) {
            School school = schoolService.getSchool(a.schoolId());
            if (this.userSchoolAffiliationRepository.countActiveByUserIdAndSchoolId(
                target.getId(),
                school.getId()
            ) > 0) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'utilisateur est déjà membre de l'école : " + schoolDisplayName(school)
                );
            }
        }
    }

    private static String schoolDisplayName(School school) {
        if (school == null) {
            return "Établissement";
        }
        if (school.getName() != null && !school.getName().isBlank()) {
            return school.getName().trim();
        }
        return school.getId() != null ? ("Établissement #" + school.getId()) : "Établissement";
    }

    private void addOrReactivateAffiliation(
        User user,
        School school,
        User.UserRole role,
        List<String> addedSchoolNames
    ) {
        Optional<UserSchoolAffiliation> opt =
            this.userSchoolAffiliationRepository.findByUser_IdAndSchool_IdAndRole(user.getId(), school.getId(), role);
        if (opt.isPresent()) {
            UserSchoolAffiliation aff = opt.get();
            if (aff.isActive()) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'utilisateur est déjà membre de l'école pour ce rôle : " + schoolDisplayName(school)
                );
            }
            aff.setActive(true);
            aff.setRole(role);
            aff.setShowInfoToTenant(true);
            this.userSchoolAffiliationRepository.save(aff);
            addedSchoolNames.add(schoolDisplayName(school));
            return;
        }
        UserSchoolAffiliation created = new UserSchoolAffiliation();
        created.setUser(user);
        created.setSchool(school);
        created.setRole(role);
        created.setActive(true);
        created.setShowInfoToTenant(true);
        this.userSchoolAffiliationRepository.save(created);
        addedSchoolNames.add(schoolDisplayName(school));
    }

    private InviteUserResponse inviteExistingUser(
        User current,
        User target,
        User.UserRole invitedRole,
        Long directorSchoolId,
        List<SchoolRoleAssignmentDTO> normalized
    ) {
        if (invitedRole == User.UserRole.ADMIN_ECOLE) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Un utilisateur avec cet e-mail existe déjà ; cette invitation ne peut pas créer un second administrateur d'organisation."
            );
        }
        if (userCapabilityService.isSuperAdmin(target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action interdite.");
        }
        if (userCapabilityService.isAdminEcole(target)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Impossible d'ajouter des rattachements d'établissement à un compte administrateur d'organisation avec cette action."
            );
        }

        InvitePlan plan = buildInvitePlan(current, invitedRole, directorSchoolId, normalized);
        if (plan.assignments.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Au moins une affectation à un établissement est requise pour un compte existant."
            );
        }

        if (shouldRecordCrossTenantPendingInvite(current, target)) {
            List<UserSchoolAffiliation> pendingAffiliations =
                createPendingCrossTenantAffiliations(target, plan.assignments);
            for (UserSchoolAffiliation aff : pendingAffiliations) {
                this.inAppNotificationService.createInvitationNotification(
                    target,
                    aff,
                    schoolDisplayName(aff.getSchool())
                );
            }
            sendCrossTenantInvitationEmail(target, assignmentSchoolNamesCsv(plan.assignments));
            return new InviteUserResponse(
                "Invitation enregistrée pour un compte rattaché à une autre organisation. "
                    + "Les coordonnées restent masquées dans votre annuaire jusqu’à acceptation.",
                null
            );
        }

        assertActiveAffiliationDuplicates(target, plan.assignments);

        assertTenantCompatibleForExistingInvite(current, target, directorSchoolId);

        if (!target.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ce compte existe mais n'est pas encore activé. Finalisez l'activation avant d'ajouter des rattachements."
            );
        }

        List<String> addedSchoolNames = new ArrayList<>();
        for (SchoolRoleAssignmentDTO a : plan.assignments) {
            School school = schoolService.getSchool(a.schoolId());
            addOrReactivateAffiliation(target, school, a.role(), addedSchoolNames);
        }

        if (invitedRole == User.UserRole.DIRECTOR) {
            target.setSchool(plan.primarySchool);
            if (plan.tenantId != null) {
                target.setTenantId(plan.tenantId);
            }
        }

        this.userRepository.save(target);

        if (!addedSchoolNames.isEmpty()) {
            this.notificationService.sendSchoolAffiliationAttachedNotice(target, addedSchoolNames);
        }

        return new InviteUserResponse(
            "Le membre existant a été rattaché aux établissements concernés ; une notification lui a été envoyée par e-mail.",
            null
        );
    }

    private InviteUserResponse inviteNewUser(
        User current,
        String emailNorm,
        User.UserRole invitedRole,
        Long directorSchoolId,
        List<SchoolRoleAssignmentDTO> normalized
    ) {
        Optional<User> existingAlready = findUserByEmailForInviteUnscoped(emailNorm);
        if (existingAlready.isPresent()) {
            return inviteExistingUser(current, existingAlready.get(), invitedRole, directorSchoolId, normalized);
        }

        InvitePlan plan = buildInvitePlan(current, invitedRole, directorSchoolId, normalized);

        User user = new User();
        user.setUsername(emailNorm);
        /* Nom affiché provisoire : la personne invitée renseignera son identité à l’activation / dans son profil. */
        user.setFullname(provisionalFullnameFromEmail(emailNorm));
        user.setEmail(emailNorm);
        user.setSchool(plan.primarySchool);
        user.setTenantId(plan.tenantId);

        user.setPassword(this.passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setActive(false);
        try {
            user = this.userRepository.save(user);
            if (plan.accountRole == User.UserRole.ADMIN_ECOLE) {
                UserPlatformRole pr = new UserPlatformRole();
                pr.setUser(user);
                pr.setRole(User.UserRole.ADMIN_ECOLE);
                user.setPlatformRole(pr);
                user = this.userRepository.save(user);
            }
        } catch (DataIntegrityViolationException ex) {
            if (isLikelyDuplicateUserEmail(ex)) {
                Optional<User> fallback = findUserByEmailForInviteUnscoped(emailNorm);
                if (fallback.isPresent()) {
                    return inviteExistingUser(current, fallback.get(), invitedRole, directorSchoolId, normalized);
                }
            }
            throw ex;
        }

        if (!plan.assignments.isEmpty()) {
            persistAffiliations(user, plan.assignments);
        }

        Activation activation = sendActivationEmail(user);
        String activationCode = activation.getCode();
        log.info("Invitation : mail d'activation envoyé à {} (code trace serveur : {})", user.getEmail(), activationCode);
        return new InviteUserResponse("Utilisateur invité avec succès.", activationCode);
    }

    private static String provisionalFullnameFromEmail(String emailNorm) {
        int at = emailNorm.indexOf('@');
        if (at > 0) {
            return emailNorm.substring(0, at);
        }
        return emailNorm.isEmpty() ? "Invité" : emailNorm;
    }

    private static List<SchoolRoleAssignmentDTO> normalizeSchoolAssignments(
        InviteUserDTO request,
        Long directorSchoolId
    ) {
        if (request.schoolAssignments() != null && !request.schoolAssignments().isEmpty()) {
            return dedupeAssignments(request.schoolAssignments());
        }
        if (directorSchoolId != null
            && request.role() != null
            && (request.role() == User.UserRole.TEACHER || request.role() == User.UserRole.STAFF)) {
            return List.of(new SchoolRoleAssignmentDTO(directorSchoolId, request.role()));
        }
        if (request.schoolId() != null
            && request.role() != null
            && request.role() != User.UserRole.ADMIN_ECOLE) {
            return List.of(new SchoolRoleAssignmentDTO(request.schoolId(), request.role()));
        }
        return List.of();
    }

    private static List<SchoolRoleAssignmentDTO> dedupeAssignments(List<SchoolRoleAssignmentDTO> list) {
        Map<String, SchoolRoleAssignmentDTO> map = new LinkedHashMap<>();
        for (SchoolRoleAssignmentDTO a : list) {
            if (a != null && a.schoolId() != null && a.role() != null) {
                map.put(a.schoolId() + ":" + a.role().name(), a);
            }
        }
        return List.copyOf(map.values());
    }

    private void persistAffiliations(User user, List<SchoolRoleAssignmentDTO> assignments) {
        for (SchoolRoleAssignmentDTO a : assignments) {
            School school = schoolService.getSchool(a.schoolId());
            UserSchoolAffiliation aff = new UserSchoolAffiliation();
            aff.setUser(user);
            aff.setSchool(school);
            aff.setRole(a.role());
            aff.setActive(true);
            aff.setShowInfoToTenant(true);
            this.userSchoolAffiliationRepository.save(aff);
        }
    }

    @Transactional
    public void activate(ActivationRequest activation) {
        if (activation.email() == null || activation.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail obligatoire.");
        }
        if (activation.activationCode() == null || activation.activationCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code d'activation obligatoire.");
        }
        if (activation.newPassword() == null || activation.newPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nouveau mot de passe obligatoire.");
        }

        Activation savedActivation = this.iActivationRepository.findByCode(activation.activationCode())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code d'activation invalide."));
        User user = this.userRepository.findByEmail(activation.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        if (user.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ce compte est déjà activé. Connectez-vous avec votre mot de passe habituel. Ignorez tout nouveau code reçu par erreur après une invitation à un établissement."
            );
        }
        if (Instant.now().isBefore(savedActivation.getExpiration())
            && savedActivation.getUser().getEmail().equals(user.getEmail())) {
            user.setPassword(this.passwordEncoder.encode(activation.newPassword()));
            user.setActive(true);
            this.userRepository.save(user);
            this.iActivationRepository.delete(savedActivation);
            this.notificationService.sendAccountActivatedMail(user);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation expirée ou non valide.");
        }
    }

    @Transactional
    public void resetPassword(Map<String, String> request) {
        User user = this.loadUserByUsername(request.get("email"));
        Activation activation = createActivation(user);
        this.notificationService.sendResetPassWordMail(activation);
    }

    @Transactional
    public void updatePassword(Map<String, String> request) {
        Activation savedActivation = this.iActivationRepository.findByCode(request.get("code"))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code d'activation invalide."));
        User user = this.userRepository.findByEmail(request.get("email"))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        if (Instant.now().isBefore(savedActivation.getExpiration())
            && savedActivation.getUser().getEmail().equals(user.getEmail())) {
            user.setPassword(this.passwordEncoder.encode(request.get("password")));
            this.userRepository.save(user);
            this.iActivationRepository.delete(savedActivation);
            this.notificationService.sendPasswordChangedConfirmationMail(user);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation expirée ou non valide.");
        }
    }

    public void saveUser(User user) {
        this.userRepository.save(user);
    }

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.userRepository
            .findByEmailWithSchoolScopes(username)
            .orElseThrow(() -> new UsernameNotFoundException("Pas d'utilisateur pour cet identifiant"));
    }

    public User getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    /**
     * Met à jour le mot de passe du compte connecté après vérification de l’ancien.
     */
    @Transactional
    public void changeOwnPassword(User principal, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe actuel requis.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nouveau mot de passe requis.");
        }
        if (newPassword.equals(currentPassword)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Le nouveau mot de passe doit être différent du mot de passe actuel."
            );
        }
        User user = this.userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        if (!this.passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe actuel est incorrect.");
        }
        user.setPassword(this.passwordEncoder.encode(newPassword));
        this.userRepository.save(user);
        this.notificationService.sendPasswordChangedConfirmationMail(user);
    }

    /**
     * Met à jour le profil du compte connecté ; {@link User#getFullname()} est synchronisé (JWT / UI).
     */
    @Transactional
    public User updateOwnProfile(User principal, OwnProfileUpdateRequest body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis.");
        }
        User user = this.userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        String lastName = trimNullToEmpty(body.lastName());
        String firstName = trimNullToEmpty(body.firstName());
        if (lastName.isEmpty()) {
            String legacy = body.fullname() != null ? body.fullname().trim() : "";
            if (!legacy.isEmpty()) {
                lastName = legacy.length() > 255 ? legacy.substring(0, 255) : legacy;
            }
        }
        if (lastName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est requis.");
        }
        if (firstName.length() > 255 || lastName.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prénom ou nom trop long.");
        }

        String genderNormalized = normalizeProfileGender(body.gender());

        String phone = trimNullToEmpty(body.phone());
        if (phone.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le numéro de téléphone est trop long.");
        }
        if (phone.isEmpty()) {
            phone = null;
        }

        String biography = body.biography() != null ? body.biography().trim() : null;
        if (biography != null && biography.length() > 12_000) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Le texte « informations complémentaires » est trop long."
            );
        }
        if (biography != null && biography.isEmpty()) {
            biography = null;
        }

        LocalDate birthDate = body.birthDate();
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de naissance ne peut pas être dans le futur.");
        }

        user.setFirstName(firstName.isEmpty() ? null : firstName);
        user.setLastName(lastName);
        user.setBirthDate(birthDate);
        user.setGender(genderNormalized);
        user.setPhone(phone);
        user.setBiography(biography);

        String composed = composeFullName(user.getFirstName(), user.getLastName());
        if (composed.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La concaténation prénom + nom dépasse 255 caractères.");
        }
        user.setFullname(composed);
        user.setUpdatedAt(Instant.now());
        return this.userRepository.save(user);
    }

    private static String trimNullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String composeFullName(String firstName, String lastName) {
        String fn = trimNullToEmpty(firstName);
        String ln = trimNullToEmpty(lastName);
        if (fn.isEmpty()) {
            return ln;
        }
        if (ln.isEmpty()) {
            return fn;
        }
        return fn + " " + ln;
    }

    /** {@code null} si vide ; sinon {@code MALE} ou {@code FEMALE}. */
    private static String normalizeProfileGender(String gender) {
        String g = gender != null ? gender.trim().toUpperCase(Locale.ROOT) : "";
        if (g.isEmpty()) {
            return null;
        }
        if ("MALE".equals(g) || "FEMALE".equals(g)) {
            return g;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valeur de sexe invalide (Homme ou Femme).");
    }

    private Activation createActivation(User user) {
        if (user.getId() != null) {
            this.iActivationRepository.deleteAllByUser_Id(user.getId());
        }
        Random random = new Random();
        Activation activation = new Activation();
        activation.setCode(String.format("%06d", random.nextInt(999999)));
        activation.setRegistrationDate(Instant.now());
        activation.setExpiration(
            Instant.now().plusMillis(NotificationService.ACTIVATION_CODE_VALIDITY_MINUTES * 60L * 1000L)
        );
        activation.setUser(user);
        return iActivationRepository.save(activation);
    }

    /**
     * Même périmètre que {@link #getAll()} : qui peut voir l’utilisateur peut lui renvoyer un code d’activation.
     */
    private void assertCurrentUserMayManageTargetUser(User current, User target) {
        if (userCapabilityService.isSuperAdmin(target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action interdite.");
        }
        if (userCapabilityService.isSuperAdmin(current)) {
            return;
        }
        if (userCapabilityService.isDirectorPrimarySchool(current)) {
            School dirSchool = current.getSchool();
            if (dirSchool == null || dirSchool.getId() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
            }
            boolean ok = this.userSchoolAffiliationRepository.existsByUser_IdAndSchool_IdAndActiveTrueAndRoleIn(
                target.getId(),
                dirSchool.getId(),
                List.of(User.UserRole.TEACHER, User.UserRole.STAFF)
            );
            if (!ok) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
            }
            schoolService.assertCurrentUserCanAccessSchool(dirSchool.getId());
            return;
        }
        if (userCapabilityService.isAdminEcole(current)) {
            Long tenantId = current.getOrganizationTenantId() != null
                ? current.getOrganizationTenantId()
                : current.getTenantId();
            Long uTenant = target.getOrganizationTenantId() != null
                ? target.getOrganizationTenantId()
                : target.getTenantId();
            if (tenantId != null && tenantId.equals(uTenant)) {
                return;
            }
            if (tenantId != null && targetHasAffiliationInFounderOrganization(target.getId(), tenantId)) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
    }

    private void assertFounderMaySyncAffiliations(User founder, User target) {
        if (userCapabilityService.isSuperAdmin(target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action interdite.");
        }
        if (userCapabilityService.isAdminEcole(target)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ce profil n’a pas d’affiliations par établissement."
            );
        }
        Long founderTenant = founder.getOrganizationTenantId() != null
            ? founder.getOrganizationTenantId()
            : founder.getTenantId();
        Long targetTenant = target.getOrganizationTenantId() != null
            ? target.getOrganizationTenantId()
            : target.getTenantId();

        if (founderTenant != null && founderTenant.equals(targetTenant)) {
            return;
        }
        /*
         * Compte rattaché à un autre tenant « maison » mais avec au moins une ligne vers nos établissements
         * (invitation cross-tenant, etc.) : le fondateur peut synchroniser comme pour un membre local.
         */
        if (founderTenant != null && targetHasAffiliationInFounderOrganization(target.getId(), founderTenant)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
    }

    private boolean targetHasAffiliationInFounderOrganization(Long targetUserId, Long founderOrgTenantId) {
        if (targetUserId == null || founderOrgTenantId == null) {
            return false;
        }
        return userSchoolAffiliationRepository.existsByUser_IdAndSchool_TenantId(targetUserId, founderOrgTenantId);
    }

    /**
     * Synchronise les affiliations actives avec la liste fournie (fondateur uniquement, contrôleur).
     * Les établissements absents de la liste sont désactivés ({@code is_active = false}).
     */
    @Transactional
    public User syncUserAffiliations(Long targetUserId, UpdateUserAffiliationsRequest request) {
        User founder = getUserInfo();
        if (founder == null || !userCapabilityService.isAdminEcole(founder)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action réservée aux administrateurs d’organisation.");
        }
        if (targetUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant utilisateur obligatoire.");
        }
        User target = this.userRepository.findById(targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        assertFounderMaySyncAffiliations(founder, target);

        List<SchoolRoleAssignmentDTO> desired = request != null && request.assignments() != null
            ? dedupeAssignments(request.assignments())
            : List.of();

        List<UserSchoolAffiliation> existingBefore =
            this.userSchoolAffiliationRepository.findAllByUser_IdJoinSchool(target.getId());
        boolean directorFamily = existingBefore.stream()
            .anyMatch(a -> a.isActive() && a.getRole() == User.UserRole.DIRECTOR);

        if (directorFamily) {
            if (desired.size() != 1 || desired.get(0).role() != User.UserRole.DIRECTOR) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Un directeur doit avoir exactement une affiliation avec le rôle DIRECTOR."
                );
            }
        } else {
            if (desired.isEmpty()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Au moins une affiliation est obligatoire pour ce profil."
                );
            }
            for (SchoolRoleAssignmentDTO d : desired) {
                if (d.role() != User.UserRole.TEACHER && d.role() != User.UserRole.STAFF) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chaque affiliation doit être TEACHER ou STAFF."
                    );
                }
            }
        }

        for (SchoolRoleAssignmentDTO d : desired) {
            this.schoolService.assertCurrentUserCanAccessSchool(d.schoolId());
        }

        Map<String, UserSchoolAffiliation> byKey = new LinkedHashMap<>();
        for (UserSchoolAffiliation a : existingBefore) {
            byKey.put(a.getSchool().getId() + ":" + a.getRole().name(), a);
        }

        Set<String> desiredKeys =
            desired.stream().map(d -> d.schoolId() + ":" + d.role().name()).collect(Collectors.toSet());

        Set<String> deactivatedThisRun = new HashSet<>();
        for (UserSchoolAffiliation aff : existingBefore) {
            String key = aff.getSchool().getId() + ":" + aff.getRole().name();
            if (!desiredKeys.contains(key)) {
                aff.setActive(false);
                aff.setAdminAccessSuspended(false);
                deactivatedThisRun.add(key);
                this.userSchoolAffiliationRepository.save(aff);
            }
        }

        for (SchoolRoleAssignmentDTO d : desired) {
            String key = d.schoolId() + ":" + d.role().name();
            UserSchoolAffiliation aff = byKey.get(key);
            if (aff == null) {
                School school = schoolService.getSchool(d.schoolId());
                UserSchoolAffiliation created = new UserSchoolAffiliation();
                created.setUser(target);
                created.setSchool(school);
                created.setRole(d.role());
                created.setActive(true);
                created.setShowInfoToTenant(true);
                created.setAdminAccessSuspended(false);
                this.userSchoolAffiliationRepository.save(created);
                byKey.put(key, created);
            } else {
                aff.setRole(d.role());
                if (aff.isAdminAccessSuspended()) {
                    /* Suspension fondateur : conserver l’accès inactif jusqu’à réactivation explicite */
                } else if (aff.isActive()) {
                    aff.setShowInfoToTenant(true);
                } else if (deactivatedThisRun.contains(key)) {
                    aff.setActive(true);
                    aff.setShowInfoToTenant(true);
                }
                /* Sinon : invitation en attente (inactive, non suspendu admin) — ne pas activer via ce flux */
                this.userSchoolAffiliationRepository.save(aff);
            }
        }

        List<UserSchoolAffiliation> refreshed =
            this.userSchoolAffiliationRepository.findAllByUser_IdJoinSchool(target.getId());

        long activeTeacherStaff = refreshed.stream()
            .filter(a -> a.isActive()
                && (a.getRole() == User.UserRole.TEACHER || a.getRole() == User.UserRole.STAFF))
            .count();
        if (!directorFamily && activeTeacherStaff < 1) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Au moins une affiliation TEACHER ou STAFF active est obligatoire."
            );
        }

        if (directorFamily) {
            long activeDir = refreshed.stream()
                .filter(a -> a.isActive() && a.getRole() == User.UserRole.DIRECTOR)
                .count();
            if (activeDir != 1) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le directeur doit avoir exactement une affiliation DIRECTOR active."
                );
            }
        }

        List<UserSchoolAffiliation> activeSorted = refreshed.stream()
            .filter(UserSchoolAffiliation::isActive)
            .sorted(Comparator.comparing(a -> a.getSchool().getId()))
            .toList();
        if (!activeSorted.isEmpty()) {
            School primary = activeSorted.get(0).getSchool();
            target.setSchool(primary);
            if (target.getTenantId() == null && target.getOrganizationTenantId() == null) {
                target.setTenantId(primary.getTenantId());
            }
        }

        target.setUpdatedAt(Instant.now());
        return this.userRepository.save(target);
    }

    /**
     * Suspend l’accès à un établissement ({@code is_active = false} sur toutes les lignes pour la paire utilisateur / école).
     */
    @Transactional
    public void suspendAffiliationAtSchool(Long targetUserId, Long schoolId) {
        User founder = getUserInfo();
        if (founder == null || !userCapabilityService.isAdminEcole(founder)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action réservée aux administrateurs d’organisation.");
        }
        if (targetUserId == null || schoolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiants obligatoires.");
        }
        User target = this.userRepository.findById(targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        assertFounderMaySyncAffiliations(founder, target);
        this.schoolService.assertCurrentUserCanAccessSchool(schoolId);

        List<UserSchoolAffiliation> rows =
            this.userSchoolAffiliationRepository.findAllByUser_IdAndSchool_Id(targetUserId, schoolId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune affiliation pour cet établissement.");
        }
        if (rows.stream().noneMatch(UserSchoolAffiliation::isActive)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette affiliation est déjà suspendue.");
        }
        for (UserSchoolAffiliation aff : rows) {
            if (aff.isActive()) {
                aff.setActive(false);
                aff.setAdminAccessSuspended(true);
                this.userSchoolAffiliationRepository.save(aff);
            }
        }
        refreshTargetPrimarySchool(target);

        School school = schoolService.getSchool(schoolId);
        String schoolName = schoolDisplayName(school);
        String title = "Accès suspendu - " + schoolName;
        String content =
            "Votre accès à l'établissement « "
                + schoolName
                + " » a été suspendu par l'administration. Vous ne pouvez plus utiliser ce contexte dans SchoolApp tant que la suspension n'est pas levée.";
        this.inAppNotificationService.createUserTargetedNotification(target, school, title, content);

        String subject = "[SchoolApp] Notification de suspension d'accès - " + schoolName;
        String htmlBody =
            """
            <html><body style="font-family:sans-serif;font-size:14px;line-height:1.5;color:#222;">
            <p>Bonjour,</p>
            <p>Votre accès à l'établissement <strong>%s</strong> a été <strong>suspendu</strong> par l'administration.</p>
            <p>Vous ne pouvez plus interagir avec cet établissement dans SchoolApp pour le moment.</p>
            <p>Pour toute question, contactez l'administration de votre organisation.</p>
            <p>Cordialement,<br/>L'équipe SchoolApp</p>
            </body></html>
            """
                .formatted(HtmlUtils.htmlEscape(schoolName));
        sendHtmlMailBestEffort(target, subject, htmlBody);
    }

    /**
     * Réactive l’accès pour les lignes suspendues (inactive avec {@code show_info_to_tenant = true}), pas les invitations en attente.
     */
    @Transactional
    public void reactivateAffiliationAtSchool(Long targetUserId, Long schoolId) {
        User founder = getUserInfo();
        if (founder == null || !userCapabilityService.isAdminEcole(founder)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action réservée aux administrateurs d’organisation.");
        }
        if (targetUserId == null || schoolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiants obligatoires.");
        }
        User target = this.userRepository.findById(targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        assertFounderMaySyncAffiliations(founder, target);
        this.schoolService.assertCurrentUserCanAccessSchool(schoolId);

        List<UserSchoolAffiliation> rows =
            this.userSchoolAffiliationRepository.findAllByUser_IdAndSchool_Id(targetUserId, schoolId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune affiliation pour cet établissement.");
        }
        boolean any = false;
        for (UserSchoolAffiliation aff : rows) {
            if (!aff.isActive() && aff.isShowInfoToTenant()) {
                aff.setActive(true);
                aff.setAdminAccessSuspended(false);
                this.userSchoolAffiliationRepository.save(aff);
                any = true;
            }
        }
        if (!any) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Aucune affiliation suspendue à réactiver pour cet établissement."
            );
        }
        refreshTargetPrimarySchool(target);

        School school = schoolService.getSchool(schoolId);
        String schoolName = schoolDisplayName(school);
        String title = "Accès réactivé - " + schoolName;
        String content =
            "Votre accès à l'établissement « "
                + schoolName
                + " » a été rétabli. Vous pouvez à nouveau sélectionner cet établissement dans la barre supérieure de SchoolApp.";
        this.inAppNotificationService.createUserTargetedNotification(target, school, title, content);

        String subject = "[SchoolApp] Votre accès a été rétabli ! - " + schoolName;
        String htmlBody =
            """
            <html><body style="font-family:sans-serif;font-size:14px;line-height:1.5;color:#222;">
            <p>Bonjour,</p>
            <p>Bonne nouvelle : votre accès à l'établissement <strong>%s</strong> a été <strong>rétabli</strong>.</p>
            <p>Vous pouvez de nouveau sélectionner cet établissement dans la <strong>barre supérieure</strong> de SchoolApp pour travailler dans ce contexte.</p>
            <p>Cordialement,<br/>L'équipe SchoolApp</p>
            </body></html>
            """
                .formatted(HtmlUtils.htmlEscape(schoolName));
        sendHtmlMailBestEffort(target, subject, htmlBody);
    }

    private void refreshTargetPrimarySchool(User target) {
        List<UserSchoolAffiliation> refreshed =
            this.userSchoolAffiliationRepository.findAllByUser_IdJoinSchool(target.getId());
        List<UserSchoolAffiliation> activeSorted = refreshed.stream()
            .filter(UserSchoolAffiliation::isActive)
            .sorted(Comparator.comparing(a -> a.getSchool().getId()))
            .toList();
        if (!activeSorted.isEmpty()) {
            School primary = activeSorted.get(0).getSchool();
            target.setSchool(primary);
            if (target.getTenantId() == null && target.getOrganizationTenantId() == null) {
                target.setTenantId(primary.getTenantId());
            }
        }
        target.setUpdatedAt(Instant.now());
        this.userRepository.save(target);
    }

    private String assignmentSchoolNamesCsv(List<SchoolRoleAssignmentDTO> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return "";
        }
        return assignments.stream()
            .map(a -> schoolDisplayName(schoolService.getSchool(a.schoolId())))
            .distinct()
            .collect(Collectors.joining(", "));
    }

    /**
     * E-mail d’invitation cross-tenant : complète les notifications in-app déjà créées.
     */
    private void sendCrossTenantInvitationEmail(User target, String schoolNamesCsv) {
        if (schoolNamesCsv == null || schoolNamesCsv.isBlank()) {
            return;
        }
        boolean plural = schoolNamesCsv.contains(",");
        String esc = HtmlUtils.htmlEscape(schoolNamesCsv);
        String intro =
            plural
                ? ("Les établissements suivants souhaitent vous ajouter à leur équipe sur SchoolApp : <strong>"
                    + esc
                    + "</strong>.")
                : ("L'établissement <strong>" + esc + "</strong> souhaite vous ajouter à son équipe sur SchoolApp.");
        String htmlBody =
            """
            <html><body style="font-family:sans-serif;font-size:14px;line-height:1.5;color:#222;">
            <p>Bonjour,</p>
            <p>%s</p>
            <p>Vos informations personnelles restent protégées. Veuillez vous connecter à votre compte et vous rendre dans votre <strong>Centre de Notifications</strong> pour accepter ou décliner cette invitation.</p>
            <p>Cordialement,<br/>L'équipe SchoolApp</p>
            </body></html>
            """
                .formatted(intro);
        String subject =
            plural
                ? "[SchoolApp] Invitation à rejoindre des établissements sur SchoolApp"
                : "[SchoolApp] Invitation à rejoindre l'établissement " + schoolNamesCsv;
        sendHtmlMailBestEffort(target, subject, htmlBody);
    }

    private void sendHtmlMailBestEffort(User user, String subject, String htmlBody) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Envoi e-mail ignoré : aucune adresse pour l'utilisateur {}", user.getId());
            return;
        }
        try {
            this.communicationMailDispatchService.sendHtml(user.getEmail().trim(), subject, htmlBody);
            log.debug("E-mail transactionnel envoyé à {}", user.getEmail());
        } catch (Exception ex) {
            log.warn("Échec envoi e-mail à {} : {}", user.getEmail(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<String> resolveEffectiveRoleNamesForProfile(User user) {
        Optional<UserPlatformRole> pr = userPlatformRoleRepository.findByUser_Id(user.getId());
        if (pr.isPresent()) {
            return List.of(pr.get().getRole().name());
        }
        List<String> roles = userSchoolAffiliationRepository.findAllByUser_IdJoinSchool(user.getId()).stream()
            .filter(UserSchoolAffiliation::isActive)
            .map(a -> a.getRole().name())
            .distinct()
            .sorted()
            .toList();
        if (!roles.isEmpty()) {
            return roles;
        }
        return List.of(User.UserRole.STAFF.name());
    }

    @Transactional(readOnly = true)
    public Set<Long> userIdsRequiringDirectoryPrivacyMask(Collection<Long> userIds, Long tenantId) {
        if (tenantId == null || userIds == null || userIds.isEmpty()) {
            return Set.of();
        }
        List<UserSchoolAffiliation> rows =
            userSchoolAffiliationRepository.findAllWithSchoolByUser_IdInAndTenantId(userIds, tenantId);
        Set<Long> out = new HashSet<>();
        for (UserSchoolAffiliation a : rows) {
            if (!a.isShowInfoToTenant()) {
                out.add(a.getUser().getId());
            }
        }
        return out;
    }

    public List<User> getAdminBySchool(Long schoolId) {
        List<User> actualList = new ArrayList<>();
        this.userRepository.findAllBySchoolId(schoolId).iterator().forEachRemaining(actualList::add);
        return actualList;
    }

    public List<User> getUnassignedUsers(String term) {
        return this.userRepository.searchByKeywordAndNoSchool(term);
    }

}

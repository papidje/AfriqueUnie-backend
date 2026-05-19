package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.auth.UserSchoolAffiliation;
import friasoft.gn.schoolapp.entity.notification.NotificationClosureReason;
import friasoft.gn.schoolapp.repository.UserSchoolAffiliationRepository;
import friasoft.gn.schoolapp.security.JwtService;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import friasoft.gn.schoolapp.tenancy.TenantHibernateFilterAspect;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserAffiliationInviteActionService {

    private final UserSchoolAffiliationRepository userSchoolAffiliationRepository;
    private final InAppNotificationService inAppNotificationService;
    private final JwtService jwtService;
    private final EntityManager entityManager;

    /**
     * Charge le rattachement par id + utilisateur sans appliquer le filtre tenant Hibernate sur {@link UserSchoolAffiliation},
     * afin de permettre acceptation / refus d’invitations vers une école d’un autre tenant que celui du JWT courant.
     */
    private Optional<UserSchoolAffiliation> findAffiliationForInviteeIgnoringSchoolTenantFilter(
        Long affiliationId,
        Long userId
    ) {
        Session session = entityManager.unwrap(Session.class);
        Filter tenantFilter = session.getEnabledFilter(TenantHibernateFilterAspect.TENANT_FILTER_NAME);
        if (tenantFilter != null) {
            session.disableFilter(TenantHibernateFilterAspect.TENANT_FILTER_NAME);
        }
        try {
            return userSchoolAffiliationRepository.findByIdAndUser_IdFetchSchool(affiliationId, userId);
        } finally {
            if (tenantFilter != null) {
                Long tenantId = TenantContext.getTenantId();
                if (tenantId != null) {
                    session.enableFilter(TenantHibernateFilterAspect.TENANT_FILTER_NAME)
                        .setParameter(TenantHibernateFilterAspect.TENANT_FILTER_PARAM, tenantId);
                }
            }
        }
    }

    @Transactional
    public Map<String, String> acceptInvitation(Long affiliationId, User principal) {
        UserSchoolAffiliation aff = findAffiliationForInviteeIgnoringSchoolTenantFilter(affiliationId, principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rattachement introuvable."));
        if (!aff.isActive()) {
            aff.setActive(true);
            aff.setShowInfoToTenant(true);
            userSchoolAffiliationRepository.save(aff);
        }
        inAppNotificationService.markInvitationNotificationsRead(
            principal.getId(),
            affiliationId,
            NotificationClosureReason.INVITATION_ACCEPTED
        );
        return jwtService.switchActiveSchool(principal.getEmail(), aff.getSchool().getId());
    }

    @Transactional
    public void refuseInvitation(Long affiliationId, User principal) {
        UserSchoolAffiliation aff = findAffiliationForInviteeIgnoringSchoolTenantFilter(affiliationId, principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rattachement introuvable."));
        if (aff.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ce rattachement est déjà actif ; utilisez la gestion des affiliations pour le retirer."
            );
        }
        inAppNotificationService.markInvitationNotificationsRead(
            principal.getId(),
            affiliationId,
            NotificationClosureReason.INVITATION_REFUSED
        );
        userSchoolAffiliationRepository.delete(aff);
    }
}

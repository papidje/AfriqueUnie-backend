package friasoft.gn.schoolapp.tenancy;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TenantHibernateFilterAspect {
    public static final String TENANT_FILTER_NAME = "tenantFilter";
    public static final String TENANT_FILTER_PARAM = "tenantId";

    /**
     * Utilisateur visible dans le tenant si : {@code tenant_id} correspond, ou école principale dans le tenant,
     * ou au moins une affiliation vers une école du tenant (y compris invitations en attente).
     */
    public static final String USER_TABLE_TENANT_FILTER_CONDITION =
        "("
            + "tenant_id = :" + TENANT_FILTER_PARAM + " OR "
            + "(school_id IS NOT NULL AND school_id IN (SELECT sch.id FROM schools.schools sch WHERE sch.tenant_id = :"
            + TENANT_FILTER_PARAM + ")) OR "
            + "id IN (SELECT usa.user_id FROM schools.user_school_affiliations usa INNER JOIN schools.schools sch ON sch.id = usa.school_id WHERE sch.tenant_id = :"
            + TENANT_FILTER_PARAM + ")"
            + ")";

    /** Rattachements utilisateur ↔ école : colonne {@code school_id} doit désigner une école du tenant courant. */
    public static final String USER_SCHOOL_AFFILIATION_TENANT_FILTER_CONDITION =
        "school_id IN (SELECT sch.id FROM schools.schools sch WHERE sch.tenant_id = :" + TENANT_FILTER_PARAM + ")";

    private final EntityManager entityManager;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || within(@org.springframework.transaction.annotation.Transactional *)")
    public Object applyTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        Long tenantId = TenantContext.getTenantId();
        Session session = entityManager.unwrap(Session.class);

        if (tenantId != null) {
            Filter filter = session.enableFilter(TENANT_FILTER_NAME);
            filter.setParameter(TENANT_FILTER_PARAM, tenantId);
        }

        try {
            return joinPoint.proceed();
        } finally {
            if (session.getEnabledFilter(TENANT_FILTER_NAME) != null) {
                session.disableFilter(TENANT_FILTER_NAME);
            }
        }
    }
}

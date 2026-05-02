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

package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
}

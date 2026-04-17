package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Parent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IParentRepository extends JpaRepository<Parent, Long> {
    Optional<Parent> findByTenantIdAndPhone(Long tenantId, String phone);
}

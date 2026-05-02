package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolRepository extends JpaRepository<School, Long> {

    List<School> findByTenantIdOrderByIdAsc(Long tenantId);
}

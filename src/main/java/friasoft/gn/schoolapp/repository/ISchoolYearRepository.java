package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.SchoolYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// SchoolYearRepository.java
@Repository
public interface ISchoolYearRepository extends JpaRepository<SchoolYear, Long> {
    List<SchoolYear> findBySchoolId(Long schoolId);
    Optional<SchoolYear> findBySchoolIdAndActiveTrue(Long schoolId);
}

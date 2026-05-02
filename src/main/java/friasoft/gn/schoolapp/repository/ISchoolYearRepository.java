package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.SchoolYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// SchoolYearRepository.java
@Repository
public interface ISchoolYearRepository extends JpaRepository<SchoolYear, Long> {

    @Query("select y from SchoolYear y join fetch y.school where y.id = :id")
    Optional<SchoolYear> findByIdWithSchool(@Param("id") Long id);

    List<SchoolYear> findBySchoolId(Long schoolId);

    /** Si plusieurs années sont marquées actives (données incohérentes), on retient la plus récente. */
    Optional<SchoolYear> findFirstBySchoolIdAndActiveTrueOrderByIdDesc(Long schoolId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SchoolYear y set y.active = false where y.school.id = :schoolId")
    void deactivateAllForSchool(@Param("schoolId") Long schoolId);
}

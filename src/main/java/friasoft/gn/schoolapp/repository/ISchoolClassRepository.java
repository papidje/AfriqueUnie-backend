package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// SchoolClassRepository.java
@Repository
public interface ISchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    List<SchoolClass> findByYear_Id(Long yearId);
    Optional<SchoolClass> findByYear_IdAndLevel_CodeAndName(Long yearId, String levelCode, String name);
}

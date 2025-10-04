package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// EnrollmentRepository.java
@Repository
public interface IEnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent_Id(Long studentId);
    List<Enrollment> findByClassRef_Id(Long classId);
    Optional<Enrollment> findByStudent_IdAndClassRef_Id(Long studentId, Long classId);
}

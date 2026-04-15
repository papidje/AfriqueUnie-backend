package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Enrollment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// EnrollmentRepository.java
@Repository
public interface IEnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent_Id(Long studentId);
    List<Enrollment> findByClassRef_Id(Long classId);
    Optional<Enrollment> findByStudent_IdAndClassRef_Id(Long studentId, Long classId);
    List<Enrollment> findAllByOrderByEnrolledOnDescIdDesc(Pageable pageable);
    List<Enrollment> findByClassRef_Year_School_IdOrderByEnrolledOnDescIdDesc(Long schoolId, Pageable pageable);

    @Query("select count(distinct e.student.id) from Enrollment e")
    long countDistinctStudents();

    @Query("select count(distinct e.student.id) from Enrollment e where e.classRef.year.school.id = :schoolId")
    long countDistinctStudentsBySchoolId(Long schoolId);
}

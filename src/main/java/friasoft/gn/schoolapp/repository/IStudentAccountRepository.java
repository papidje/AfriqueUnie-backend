package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.StudentAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IStudentAccountRepository extends JpaRepository<StudentAccount, Long> {
    Optional<StudentAccount> findByStudent_IdAndSchoolYear_Id(Long studentId, Long schoolYearId);
    List<StudentAccount> findByStudent_IdInAndSchoolYear_Id(Collection<Long> studentIds, Long schoolYearId);
}


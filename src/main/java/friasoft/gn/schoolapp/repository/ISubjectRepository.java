package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ISubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByCodeIgnoreCase(String code);
}

package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;

// StudentRepository.java
@Repository
public interface IStudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByLastNameContainingIgnoreCase(String lastName);
}

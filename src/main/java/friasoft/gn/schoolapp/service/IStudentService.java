package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IStudentService {
    Page<Student> findAll(Pageable pageable);
    Optional<Student> findById(Long id);
    List<Student> searchByLastName(String lastName);
    Student save(Student student);
    void delete(Long id);
}

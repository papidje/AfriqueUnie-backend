package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// StudentService.java
@Service
@AllArgsConstructor
public class StudentService implements IStudentService {

    private final IStudentRepository repository;

    @Override
    public Student save(Student student) {
        student.setMatricule(student.buildMatricule());
        return repository.save(student);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Student> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Optional<Student> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Student> searchByLastName(String lastName) {
        return repository.findByLastNameContainingIgnoreCase(lastName);
    }
}

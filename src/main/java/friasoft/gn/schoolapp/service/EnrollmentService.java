package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.Enrollment;
import friasoft.gn.schoolapp.repository.IEnrollmentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// EnrollmentService.java
@Service
@AllArgsConstructor
public class EnrollmentService {

    private final IEnrollmentRepository repository;

    public Enrollment save(Enrollment enrollment) {
        return repository.save(enrollment);
    }

    public Optional<Enrollment> findById(Long id) {
        return repository.findById(id);
    }

    public List<Enrollment> findByStudent(Long studentId) {
        return repository.findByStudent_Id(studentId);
    }

    public List<Enrollment> findByClass(Long classId) {
        return repository.findByClassRef_Id(classId);
    }
}

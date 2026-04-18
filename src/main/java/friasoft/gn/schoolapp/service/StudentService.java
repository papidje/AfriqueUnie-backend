package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// StudentService.java
@Service
@AllArgsConstructor
public class StudentService implements IStudentService {

    private final IStudentRepository repository;
    private final ISchoolClassRepository schoolClassRepository;
    private final SchoolService schoolService;
    private final UserRepository userRepository;

    private static User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new IllegalStateException("Utilisateur non authentifié.");
        }
        return u;
    }

    @Override
    public Student save(Student student) {
        student.setMatricule(student.buildMatricule());
        return repository.save(student);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Page<Student> findAll(Pageable pageable) {
        User user = currentUser();
        if (user.getRole() == User.UserRole.DIRECTOR) {
            Long sid = userRepository.findSchoolIdByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("École assignée manquante pour le directeur."));
            return repository.findAllBySchoolId(sid, pageable);
        }
        return repository.findAll(pageable);
    }

    @Override
    public Optional<Student> findById(Long id) {
        Optional<Student> opt = repository.findById(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        Student s = opt.get();
        if (s.getSchoolClass() != null && s.getSchoolClass().getYear() != null
            && s.getSchoolClass().getYear().getSchool() != null) {
            schoolService.assertCurrentUserCanAccessSchool(s.getSchoolClass().getYear().getSchool().getId());
        }
        return opt;
    }

    @Override
    public List<Student> searchByLastName(String lastName) {
        User user = currentUser();
        List<Student> all = repository.findByLastNameContainingIgnoreCase(lastName);
        if (user.getRole() != User.UserRole.DIRECTOR) {
            return all;
        }
        Long sid = userRepository.findSchoolIdByUserId(user.getId()).orElse(null);
        if (sid == null) {
            return List.of();
        }
        final Long schoolId = sid;
        return all.stream()
            .filter(st -> st.getSchoolClass() != null && st.getSchoolClass().getYear() != null
                && st.getSchoolClass().getYear().getSchool() != null
                && schoolId.equals(st.getSchoolClass().getYear().getSchool().getId()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> findByClass(Long classId) {
        if (classId == null) {
            throw new IllegalArgumentException("classId obligatoire.");
        }
        var sc = schoolClassRepository.findByIdWithYearAndSchool(classId)
            .orElseThrow(() -> new IllegalArgumentException("SchoolClass introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(sc.getYear().getSchool().getId());
        return repository.findBySchoolClass_Id(classId);
    }
}

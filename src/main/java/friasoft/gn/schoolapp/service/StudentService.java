package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.request.StudentPatchRequest;
import friasoft.gn.schoolapp.dto.request.StudentProfileUpdateRequest;
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
    @Transactional
    public Student save(Student student) {
        if (student.getId() == null) {
            student.setMatricule(student.buildMatricule());
        }
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
    @Transactional(readOnly = true)
    public Optional<Student> findById(Long id) {
        Optional<Student> opt = repository.findByIdWithParentsAndClass(id);
        opt.ifPresent(this::assertSchoolAccess);
        return opt;
    }

    @Override
    @Transactional
    public Student updateProfile(Long id, StudentProfileUpdateRequest request) {
        Student student = loadStudentForUpdate(id);
        if (request.civility() != null && !request.civility().isBlank()) {
            student.setCivility(Student.Civility.valueOf(request.civility().trim().toUpperCase()));
        }
        student.setFirstName(requireNonBlank(request.firstName(), "Prénom obligatoire."));
        student.setLastName(requireNonBlank(request.lastName(), "Nom obligatoire."));
        if (request.birthDate() == null) {
            throw new IllegalArgumentException("Date de naissance obligatoire.");
        }
        student.setBirthDate(request.birthDate());
        student.setEmergencyContactName(requireNonBlank(request.emergencyContactName(), "Contact d'urgence (nom) obligatoire."));
        student.setEmergencyContactPhone(requireNonBlank(request.emergencyContactPhone(), "Contact d'urgence (téléphone) obligatoire."));
        return repository.save(student);
    }

    @Override
    @Transactional
    public Student patchStudent(Long id, StudentPatchRequest request) {
        Student student = loadStudentForUpdate(id);
        if (request.civility() != null) student.setCivility(Student.Civility.valueOf(request.civility().trim().toUpperCase()));
        if (request.firstName() != null) student.setFirstName(request.firstName().trim());
        if (request.lastName() != null) student.setLastName(request.lastName().trim());
        if (request.birthDate() != null) student.setBirthDate(request.birthDate());
        if (request.birthPlace() != null) student.setBirthPlace(trimToNull(request.birthPlace()));
        if (request.nationality() != null) student.setNationality(trimToNull(request.nationality()));
        if (request.address() != null) student.setAddress(trimToNull(request.address()));
        if (request.communicationPhone() != null) student.setCommunicationPhone(trimToNull(request.communicationPhone()));
        if (request.communicationEmail() != null) student.setCommunicationEmail(trimToNull(request.communicationEmail()));
        if (request.emergencyContactName() != null) student.setEmergencyContactName(trimToNull(request.emergencyContactName()));
        if (request.emergencyContactPhone() != null) student.setEmergencyContactPhone(trimToNull(request.emergencyContactPhone()));
        if (request.bloodGroup() != null) student.setBloodGroup(trimToNull(request.bloodGroup()));
        if (request.allergies() != null) student.setAllergies(trimToNull(request.allergies()));
        if (request.tutorName() != null) student.setTutorName(trimToNull(request.tutorName()));
        if (request.tutorProfession() != null) student.setTutorProfession(trimToNull(request.tutorProfession()));
        if (request.tutorPhone() != null) student.setTutorPhone(trimToNull(request.tutorPhone()));
        if (request.tutorEmail() != null) student.setTutorEmail(trimToNull(request.tutorEmail()));
        if (request.classHistory() != null) student.setClassHistory(trimToNull(request.classHistory()));
        if (request.enrollmentStatus() != null && !request.enrollmentStatus().isBlank()) {
            student.setEnrollmentStatus(Student.EnrollmentStatus.valueOf(request.enrollmentStatus().trim().toUpperCase()));
        }
        return repository.save(student);
    }

    @Override
    @Transactional
    public Student updatePhotoPath(Long id, String photoPath) {
        Student student = loadStudentForUpdate(id);
        student.setPhotoPath(trimToNull(photoPath));
        return repository.save(student);
    }

    @Override
    @Transactional
    public void unlinkFather(Long studentId) {
        Student student = loadStudentForUpdate(studentId);
        student.setFather(null);
        repository.save(student);
    }

    @Override
    @Transactional
    public void unlinkMother(Long studentId) {
        Student student = loadStudentForUpdate(studentId);
        student.setMother(null);
        repository.save(student);
    }

    private Student loadStudentForUpdate(Long studentId) {
        Student student = repository.findByIdWithParentsAndClass(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Élève introuvable."));
        assertSchoolAccess(student);
        return student;
    }

    private void assertSchoolAccess(Student student) {
        if (student.getSchoolClass() != null && student.getSchoolClass().getYear() != null
            && student.getSchoolClass().getYear().getSchool() != null) {
            schoolService.assertCurrentUserCanAccessSchool(student.getSchoolClass().getYear().getSchool().getId());
        }
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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

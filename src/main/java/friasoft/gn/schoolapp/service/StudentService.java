package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.request.StudentPatchRequest;
import friasoft.gn.schoolapp.dto.request.StudentProfileUpdateRequest;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IGradeRepository;
import friasoft.gn.schoolapp.repository.IPaymentRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import friasoft.gn.schoolapp.security.SecurityAuthorityUtils;
import friasoft.gn.schoolapp.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentService implements IStudentService {

    private final IStudentRepository repository;
    private final ISchoolClassRepository schoolClassRepository;
    private final IPaymentRepository paymentRepository;
    private final IGradeRepository gradeRepository;
    private final SchoolService schoolService;
    private final UserRepository userRepository;
    private final TeacherTimetableAccessService teacherTimetableAccessService;
    private final FileStorageService fileStorageService;

    private static User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new IllegalStateException("Utilisateur non authentifié.");
        }
        return u;
    }

    private static boolean currentUserHasTeacherAuthority() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));
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
    @Transactional
    public void delete(Long id) {
        Student student = loadStudentForUpdate(id);
        if (paymentRepository.existsByStudentAccount_Student_Id(id)) {
            throw new IllegalStateException(
                "Suppression impossible : des paiements existent pour cet élève. Utilisez la désinscription."
            );
        }
        if (gradeRepository.existsByStudent_Id(id)) {
            throw new IllegalStateException(
                "Suppression impossible : des notes existent pour cet élève. Utilisez la désinscription."
            );
        }
        String photoPath = student.getPhotoPath();
        repository.delete(student);
        fileStorageService.deleteStudentPhotoIfPresent(photoPath);
    }

    @Override
    public Page<Student> findAll(Pageable pageable) {
        User user = currentUser();
        Optional<Set<Long>> teacherClassIds = teacherTimetableAccessService.allowedSchoolClassIdsForCurrentUser();
        if (teacherClassIds.isPresent()) {
            Set<Long> ids = teacherClassIds.get();
            if (ids.isEmpty()) {
                return Page.empty(pageable);
            }
            return repository.findAllBySchoolClassIdIn(ids, pageable);
        }
        if (SecurityAuthorityUtils.hasAuthority(SecurityAuthorityUtils.ROLE_DIRECTOR)) {
            Long sid = userRepository.findSchoolIdByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("École assignée manquante pour le directeur."));
            return repository.findAllBySchoolId(sid, pageable);
        }
        return repository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Student> findById(Long id) {
        return repository.findByIdWithParentsAndClass(id)
            .map(s -> {
                assertSchoolAccess(s);
                return s;
            })
            .filter(teacherTimetableAccessService::isStudentVisibleInTeacherView);
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
    public Student transferToClass(Long studentId, Long targetClassId) {
        if (targetClassId == null) {
            throw new IllegalArgumentException("classId obligatoire.");
        }
        Student student = loadStudentForUpdate(studentId);
        if (student.getEnrollmentStatus() == Student.EnrollmentStatus.DESINSCRIT) {
            // Réinscription via affectation à une classe
        }
        SchoolClass target = schoolClassRepository.findByIdWithYearAndSchool(targetClassId)
            .orElseThrow(() -> new IllegalArgumentException("Classe introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(target.getYear().getSchool().getId());

        Long studentSchoolId = resolveSchoolId(student);
        Long targetSchoolId = target.getYear().getSchool().getId();
        if (studentSchoolId != null && !studentSchoolId.equals(targetSchoolId)) {
            throw new IllegalArgumentException("La classe cible n'appartient pas au même établissement.");
        }

        String fromLabel = student.getSchoolClass() != null ? student.getSchoolClass().getName() : "sans classe";
        String toLabel = target.getName();
        student.setSchoolClass(target);
        student.setSchool(target.getYear().getSchool());
        student.setEnrollmentStatus(Student.EnrollmentStatus.INSCRIT);
        appendClassHistory(student, "Transfert " + fromLabel + " → " + toLabel + " (" + LocalDate.now() + ")");
        return repository.save(student);
    }

    @Override
    @Transactional
    public Student unassignFromClass(Long studentId) {
        Student student = loadStudentForUpdate(studentId);
        if (student.getSchoolClass() == null) {
            throw new IllegalArgumentException("L'élève n'est déjà rattaché à aucune classe.");
        }
        if (student.getEnrollmentStatus() == Student.EnrollmentStatus.DESINSCRIT) {
            throw new IllegalArgumentException("Élève désinscrit : réaffectez-le via un transfert vers une classe.");
        }
        ensureSchoolAnchored(student);
        String fromLabel = student.getSchoolClass().getName();
        student.setSchoolClass(null);
        student.setEnrollmentStatus(Student.EnrollmentStatus.SANS_CLASSE);
        appendClassHistory(student, "Désaffectation de " + fromLabel + " (" + LocalDate.now() + ")");
        return repository.save(student);
    }

    @Override
    @Transactional
    public Student unenroll(Long studentId) {
        Student student = loadStudentForUpdate(studentId);
        ensureSchoolAnchored(student);
        String fromLabel = student.getSchoolClass() != null ? student.getSchoolClass().getName() : "sans classe";
        student.setSchoolClass(null);
        student.setEnrollmentStatus(Student.EnrollmentStatus.DESINSCRIT);
        appendClassHistory(student, "Désinscription (départ) depuis " + fromLabel + " (" + LocalDate.now() + ")");
        return repository.save(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> findUnassignedBySchool(Long schoolId) {
        if (schoolId == null) {
            throw new IllegalArgumentException("schoolId obligatoire.");
        }
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return repository.findUnassignedBySchoolId(schoolId);
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
        teacherTimetableAccessService.assertCurrentTeacherCanViewStudentOrElseForbidden(student);
        return student;
    }

    private void assertSchoolAccess(Student student) {
        Long schoolId = resolveSchoolId(student);
        if (schoolId != null) {
            schoolService.assertCurrentUserCanAccessSchool(schoolId);
        }
    }

    private static Long resolveSchoolId(Student student) {
        if (student.getSchool() != null && student.getSchool().getId() != null) {
            return student.getSchool().getId();
        }
        if (student.getSchoolClass() != null && student.getSchoolClass().getYear() != null
            && student.getSchoolClass().getYear().getSchool() != null) {
            return student.getSchoolClass().getYear().getSchool().getId();
        }
        return null;
    }

    private void ensureSchoolAnchored(Student student) {
        if (student.getSchool() != null && student.getSchool().getId() != null) {
            return;
        }
        if (student.getSchoolClass() != null
            && student.getSchoolClass().getYear() != null
            && student.getSchoolClass().getYear().getSchool() != null) {
            student.setSchool(student.getSchoolClass().getYear().getSchool());
            return;
        }
        throw new IllegalStateException(
            "Impossible de déterminer l'établissement de l'élève. Réaffectez-le d'abord à une classe."
        );
    }

    private static void appendClassHistory(Student student, String line) {
        String prev = student.getClassHistory();
        if (prev == null || prev.isBlank()) {
            student.setClassHistory(line);
        } else {
            student.setClassHistory(prev.trim() + "\n" + line);
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
        if (currentUserHasTeacherAuthority()) {
            Optional<Set<Long>> tClasses = teacherTimetableAccessService.allowedSchoolClassIdsForCurrentUser();
            if (tClasses.isPresent()) {
                Set<Long> set = tClasses.get();
                if (set.isEmpty()) {
                    return List.of();
                }
                return all.stream()
                    .filter(st -> st.getSchoolClass() != null && st.getSchoolClass().getId() != null
                        && set.contains(st.getSchoolClass().getId()))
                    .toList();
            }
        }
        if (!SecurityAuthorityUtils.hasAuthority(SecurityAuthorityUtils.ROLE_DIRECTOR)) {
            return all;
        }
        Long sid = userRepository.findSchoolIdByUserId(user.getId()).orElse(null);
        if (sid == null) {
            return List.of();
        }
        final Long schoolId = sid;
        return all.stream()
            .filter(st -> schoolId.equals(resolveSchoolId(st)))
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
        teacherTimetableAccessService.assertCurrentTeacherCanAccessClassOrElseForbidden(classId);
        return repository.findBySchoolClass_Id(classId);
    }
}

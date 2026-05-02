package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ClassSubjectDtos.ClassPlanningView;
import friasoft.gn.schoolapp.dto.ClassSubjectDtos.ClassSubjectResponse;
import friasoft.gn.schoolapp.dto.ClassSubjectDtos.CreateClassSubjectRequest;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.ClassSubject;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.Subject;
import friasoft.gn.schoolapp.repository.IClassSubjectRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.ISubjectRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ClassSubjectService {

    private final IClassSubjectRepository classSubjectRepository;
    private final ISchoolClassRepository schoolClassRepository;
    private final ISubjectRepository subjectRepository;
    private final SubjectService subjectService;
    private final UserRepository userRepository;
    private final SchoolService schoolService;

    @Transactional(readOnly = true)
    public ClassPlanningView getPlanning(Long classId) {
        SchoolClass clazz = loadClassForAccess(classId);
        assertAccess(clazz);
        Long schoolId = clazz.getYear().getSchool().getId();
        List<ClassSubjectResponse> subjects = classSubjectRepository.findBySchoolClassIdWithSubject(classId).stream()
            .map(cs -> toResponse(cs, schoolId))
            .toList();
        return new ClassPlanningView(classId, clazz.getName(), schoolId, subjects);
    }

    @Transactional(readOnly = true)
    public List<ClassSubjectResponse> listForClass(Long classId) {
        SchoolClass clazz = loadClassForAccess(classId);
        assertAccess(clazz);
        Long schoolId = clazz.getYear().getSchool().getId();
        return classSubjectRepository.findBySchoolClassIdWithSubject(classId).stream()
            .map(cs -> toResponse(cs, schoolId))
            .toList();
    }

    @Transactional
    public ClassSubjectResponse create(Long classId, CreateClassSubjectRequest request) {
        SchoolClass clazz = loadClassForAccess(classId);
        assertAccess(clazz);
        if (request.subjectId() == null) {
            throw new IllegalArgumentException("subjectId est obligatoire.");
        }
        Integer coeff = request.coefficient() != null ? request.coefficient() : 1;
        if (coeff < 1 || coeff > 20) {
            throw new IllegalArgumentException("Le coefficient doit être entre 1 et 20.");
        }
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new IllegalArgumentException("Matière introuvable."));
        subjectService.assertSubjectAssignableToSchool(subject, clazz.getYear().getSchool().getId());
        if (classSubjectRepository.findBySchoolClass_IdAndSubject_Id(classId, subject.getId()).isPresent()) {
            throw new IllegalArgumentException("Cette matière est déjà affectée à la classe.");
        }
        ClassSubject cs = new ClassSubject();
        cs.setSchoolClass(clazz);
        cs.setSubject(subject);
        cs.setCoefficient(coeff);
        cs.setTenantId(clazz.getTenantId());
        if (request.teacherId() != null) {
            cs.setTeacher(loadValidatedTeacher(request.teacherId(), clazz));
        }
        try {
            ClassSubject saved = classSubjectRepository.save(cs);
            return toResponse(saved, clazz.getYear().getSchool().getId());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Affectation déjà existante ou données invalides.", e);
        }
    }

    @Transactional
    public ClassSubjectResponse updateCoefficient(Long id, Integer coefficient) {
        if (coefficient == null || coefficient < 1 || coefficient > 20) {
            throw new IllegalArgumentException("Le coefficient doit être entre 1 et 20.");
        }
        ClassSubject cs = classSubjectRepository.findByIdForManagement(id)
            .orElseThrow(() -> new IllegalArgumentException("Liaison introuvable."));
        assertAccess(cs.getSchoolClass());
        cs.setCoefficient(coefficient);
        return toResponse(classSubjectRepository.save(cs), cs.getSchoolClass().getYear().getSchool().getId());
    }

    @Transactional
    public ClassSubjectResponse assignTeacher(Long id, Long teacherId) {
        ClassSubject cs = classSubjectRepository.findByIdForManagement(id)
            .orElseThrow(() -> new IllegalArgumentException("Liaison introuvable."));
        SchoolClass clazz = cs.getSchoolClass();
        assertAccess(clazz);
        if (teacherId == null) {
            cs.setTeacher(null);
        } else {
            cs.setTeacher(loadValidatedTeacher(teacherId, clazz));
        }
        return toResponse(classSubjectRepository.save(cs), clazz.getYear().getSchool().getId());
    }

    @Transactional
    public void delete(Long id) {
        ClassSubject cs = classSubjectRepository.findByIdForManagement(id)
            .orElseThrow(() -> new IllegalArgumentException("Liaison introuvable."));
        assertAccess(cs.getSchoolClass());
        classSubjectRepository.delete(cs);
    }

    private SchoolClass loadClassForAccess(Long classId) {
        return schoolClassRepository.findByIdWithYearAndSchool(classId)
            .orElseThrow(() -> new IllegalArgumentException("Classe introuvable."));
    }

    private void assertAccess(SchoolClass clazz) {
        schoolService.assertCurrentUserCanAccessSchool(clazz.getYear().getSchool().getId());
    }

    private User loadValidatedTeacher(Long teacherId, SchoolClass clazz) {
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        if (teacher.getRole() != User.UserRole.TEACHER) {
            throw new IllegalArgumentException("Seul un utilisateur avec le rôle TEACHER peut être assigné.");
        }
        School school = clazz.getYear().getSchool();
        if (userRepository.countTeacherAssignableToSchool(teacher.getId(), school.getId(), school.getTenantId()) < 1) {
            throw new IllegalArgumentException("Ce professeur n’est pas rattaché à l’établissement de la classe.");
        }
        return teacher;
    }

    private ClassSubjectResponse toResponse(ClassSubject cs, Long schoolId) {
        Subject s = cs.getSubject();
        User t = cs.getTeacher();
        Long tid = t != null ? t.getId() : null;
        String tname = t != null ? t.getFullname() : null;
        return new ClassSubjectResponse(
            cs.getId(),
            cs.getSchoolClass().getId(),
            schoolId,
            s.getId(),
            s.getCode(),
            s.getName(),
            cs.getCoefficient(),
            tid,
            tname
        );
    }
}

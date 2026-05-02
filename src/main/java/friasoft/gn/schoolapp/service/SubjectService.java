package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.Subject;
import friasoft.gn.schoolapp.repository.ISubjectRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SubjectService {

    private final ISubjectRepository repository;
    private final SchoolService schoolService;

    @Transactional(readOnly = true)
    public List<Subject> findCatalogForSchool(Long schoolId) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return repository.findCatalogForSchool(schoolId);
    }

    @Transactional(readOnly = true)
    public Optional<Subject> findInCatalog(Long schoolId, Long subjectId) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return repository.findByIdInSchoolCatalog(subjectId, schoolId);
    }

    @Transactional
    public Subject createForSchool(Long schoolId, Subject input) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        School school = schoolService.getSchool(schoolId);
        trimFields(input);
        validateRequired(input);
        input.setId(null);
        input.setSchool(school);
        assertCodeUniqueInCatalogScope(input.getCode(), schoolId, null);
        try {
            return repository.save(input);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Code matière déjà utilisé pour cette portée.", e);
        }
    }

    @Transactional
    public Subject updateInCatalog(Long schoolId, Long id, Subject input) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        Subject existing = repository.findByIdInSchoolCatalog(id, schoolId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matière introuvable pour cet établissement.")
            );
        assertSchoolSpecificSubjectForMutation(existing, false);
        trimFields(input);
        validateRequired(input);
        assertCodeUniqueInCatalogScope(input.getCode(), schoolId, id);
        existing.setCode(input.getCode());
        existing.setName(input.getName());
        try {
            return repository.save(existing);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Code matière déjà utilisé pour cette portée.", e);
        }
    }

    @Transactional
    public void deleteInCatalog(Long schoolId, Long id) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        Subject existing = repository.findByIdInSchoolCatalog(id, schoolId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matière introuvable pour cet établissement.")
            );
        assertSchoolSpecificSubjectForMutation(existing, true);
        try {
            repository.delete(existing);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Impossible de supprimer : la matière est affectée à une ou plusieurs classes.", e);
        }
    }

    /** Le référentiel global ({@code school_id} NULL) n’est pas modifiable ni supprimable depuis l’app. */
    private static void assertSchoolSpecificSubjectForMutation(Subject subject, boolean delete) {
        if (subject.getSchool() != null) {
            return;
        }
        if (delete) {
            throw new IllegalArgumentException("Les matières du référentiel commun ne peuvent pas être supprimées.");
        }
        throw new IllegalArgumentException("Les matières du référentiel commun ne peuvent pas être modifiées.");
    }

    /**
     * Une matière peut être affectée à une classe si elle est « visible » pour l’établissement de la classe
     * (référentiel global ou matière propre à cet établissement).
     */
    public void assertSubjectAssignableToSchool(Subject subject, Long schoolId) {
        if (subject.getSchool() == null) {
            return;
        }
        if (!subject.getSchool().getId().equals(schoolId)) {
            throw new IllegalArgumentException("Cette matière n’appartient pas à l’établissement de la classe.");
        }
    }

    private void assertCodeUniqueInCatalogScope(String code, Long schoolId, Long excludeId) {
        List<Subject> found = repository.findByCodeInCatalogScope(code, schoolId);
        for (Subject s : found) {
            if (excludeId == null || !s.getId().equals(excludeId)) {
                throw new IllegalArgumentException("Une matière avec ce code existe déjà dans le référentiel visible pour cet établissement.");
            }
        }
    }

    private static void trimFields(Subject subject) {
        if (subject.getCode() != null) {
            subject.setCode(subject.getCode().trim());
        }
        if (subject.getName() != null) {
            subject.setName(subject.getName().trim());
        }
    }

    private static void validateRequired(Subject subject) {
        if (subject.getCode() == null || subject.getCode().isEmpty()) {
            throw new IllegalArgumentException("Le code est obligatoire.");
        }
        if (subject.getName() == null || subject.getName().isEmpty()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
    }
}

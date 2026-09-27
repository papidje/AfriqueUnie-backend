package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.Subject;
import friasoft.gn.schoolapp.repository.IClassLevelGroupRepository;
import friasoft.gn.schoolapp.repository.IClassSubjectRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.ISubjectRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class SubjectService {

    private final ISubjectRepository repository;
    private final SchoolService schoolService;
    private final IClassSubjectRepository classSubjectRepository;
    private final ISchoolClassRepository schoolClassRepository;
    private final IClassLevelGroupRepository classLevelGroupRepository;

    @Transactional(readOnly = true)
    public List<Subject> findCatalogForSchool(Long schoolId) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return repository.findCatalogForSchool(schoolId);
    }

    /**
     * Catalogue visible pour l’école et compatible avec le groupe de cycle de la classe.
     */
    @Transactional(readOnly = true)
    public List<Subject> findCatalogAssignableToClass(Long classId) {
        SchoolClass clazz = schoolClassRepository.findByIdWithYearAndSchool(classId)
            .orElseThrow(() -> new IllegalArgumentException("Classe introuvable."));
        Long schoolId = clazz.getYear().getSchool().getId();
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        if (clazz.getLevel() == null || clazz.getLevel().getGroup() == null) {
            throw new IllegalArgumentException("Niveau de classe incomplet.");
        }
        return repository.findCatalogForSchoolAndLevelGroup(schoolId, clazz.getLevel().getGroup().getId());
    }

    @Transactional(readOnly = true)
    public Optional<Subject> findInCatalog(Long schoolId, Long subjectId) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return repository.findByIdInSchoolCatalog(subjectId, schoolId);
    }

    @Transactional
    public Subject createForSchool(Long schoolId, Subject input) {
        throw new IllegalArgumentException(
            "La création libre de matières par établissement est désactivée. "
                + "Utilisez une demande d’ajout au référentiel (« Nouvelle matière »)."
        );
    }

    @Transactional
    public Subject updateInCatalog(Long schoolId, Long id, Subject input) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        Subject existing = repository.findByIdInSchoolCatalog(id, schoolId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matière introuvable pour cet établissement.")
            );
        assertSchoolSpecificSubjectForMutation(existing, false);
        throw new IllegalArgumentException(
            "Les matières locales ne sont plus modifiables. Demandez l’ajout au référentiel commun si besoin."
        );
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

    /**
     * Vérifie que la matière est autorisée pour le groupe de cycle de la classe.
     */
    public void assertSubjectCompatibleWithClassLevelGroup(Subject subject, SchoolClass clazz) {
        if (clazz.getLevel() == null || clazz.getLevel().getGroup() == null) {
            throw new IllegalArgumentException("Niveau de classe incomplet.");
        }
        ClassLevelGroup group = clazz.getLevel().getGroup();
        Subject loaded = repository.findByIdWithLevelGroups(subject.getId()).orElse(subject);
        Set<ClassLevelGroup> allowed = loaded.getLevelGroups();
        if (allowed == null || allowed.isEmpty()) {
            throw new IllegalArgumentException(
                "Cette matière n’est rattachée à aucun cycle scolaire et ne peut pas être affectée."
            );
        }
        boolean ok = allowed.stream().anyMatch(g -> g.getId() != null && g.getId().equals(group.getId()));
        if (!ok) {
            throw new IllegalArgumentException(
                "La matière « " + subject.getName() + " » n’est pas enseignée au cycle « "
                    + group.getName() + " »."
            );
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

    // —— Référentiel global (SuperAdmin) ——

    @Transactional(readOnly = true)
    public List<Subject> listGlobalSubjects() {
        return repository.findGlobalSubjects();
    }

    @Transactional
    public Subject createGlobal(String codeRaw, String nameRaw) {
        return createGlobal(codeRaw, nameRaw, null);
    }

    /**
     * @param levelGroupCodes si null ou vide → tous les cycles ; sinon uniquement ces codes.
     */
    @Transactional
    public Subject createGlobal(String codeRaw, String nameRaw, List<String> levelGroupCodes) {
        Subject input = new Subject();
        input.setCode(codeRaw);
        input.setName(nameRaw);
        trimFields(input);
        validateRequired(input);
        if (!repository.findGlobalByCode(input.getCode()).isEmpty()) {
            throw new IllegalStateException("Une matière globale avec ce code existe déjà.");
        }
        input.setId(null);
        input.setSchool(null);
        Set<ClassLevelGroup> groups;
        if (levelGroupCodes == null || levelGroupCodes.isEmpty()) {
            groups = new HashSet<>(classLevelGroupRepository.findAll());
        } else {
            groups = new HashSet<>();
            for (String raw : levelGroupCodes) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String code = raw.trim().toUpperCase();
                ClassLevelGroup g = classLevelGroupRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("Groupe de niveau inconnu : " + code));
                groups.add(g);
            }
            if (groups.isEmpty()) {
                throw new IllegalArgumentException("Au moins un cycle scolaire est obligatoire.");
            }
        }
        input.replaceLevelGroups(groups);
        try {
            return repository.save(input);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Code matière déjà utilisé.", e);
        }
    }

    @Transactional
    public Subject updateGlobal(Long id, String codeRaw, String nameRaw) {
        Subject existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Matière introuvable."));
        if (existing.getSchool() != null) {
            throw new IllegalArgumentException("Cette matière n’appartient pas au référentiel global.");
        }
        Subject input = new Subject();
        input.setCode(codeRaw);
        input.setName(nameRaw);
        trimFields(input);
        validateRequired(input);
        for (Subject s : repository.findGlobalByCode(input.getCode())) {
            if (!s.getId().equals(id)) {
                throw new IllegalStateException("Une matière globale avec ce code existe déjà.");
            }
        }
        existing.setCode(input.getCode());
        existing.setName(input.getName());
        try {
            return repository.save(existing);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Code matière déjà utilisé.", e);
        }
    }

    @Transactional
    public void deleteGlobal(Long id) {
        Subject existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Matière introuvable."));
        if (existing.getSchool() != null) {
            throw new IllegalArgumentException("Cette matière n’appartient pas au référentiel global.");
        }
        if (classSubjectRepository.existsBySubject_Id(id)) {
            throw new IllegalStateException(
                "Impossible de supprimer : la matière est affectée à une ou plusieurs classes."
            );
        }
        repository.delete(existing);
    }
}

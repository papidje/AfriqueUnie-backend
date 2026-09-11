package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.SchoolClassDtos.UpdateSchoolClassRequest;
import friasoft.gn.schoolapp.dto.response.SchoolClassOverviewResponse;
import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.entity.school.PeriodType;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.repository.IClassLevelRepository;
import friasoft.gn.schoolapp.repository.IClassSubjectRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.ISchoolYearRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.util.ClassLevelOrdering;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final ISchoolClassRepository repository;
    private final ISchoolYearRepository schoolYearRepository;
    private final IClassLevelRepository classLevelRepository;
    private final IStudentRepository studentRepository;
    private final IClassSubjectRepository classSubjectRepository;
    private final SchoolService schoolService;
    private final TeacherTimetableAccessService teacherTimetableAccessService;
    private final GradingPeriodService gradingPeriodService;

    @Transactional
    public SchoolClass save(SchoolClass schoolClass) {
        boolean isNew = schoolClass.getId() == null;
        if (schoolClass.getYear() == null || schoolClass.getYear().getId() == null) {
            throw new IllegalArgumentException("year.id est obligatoire.");
        }
        if (schoolClass.getLevel() == null || schoolClass.getLevel().getId() == null) {
            throw new IllegalArgumentException("level.id est obligatoire.");
        }

        SchoolYear year = schoolYearRepository.findByIdWithSchool(schoolClass.getYear().getId())
            .orElseThrow(() -> new IllegalArgumentException("Année scolaire introuvable."));

        schoolService.assertCurrentUserCanAccessSchool(year.getSchool().getId());

        Long tenantId = year.getTenantId();
        if (tenantId == null) {
            tenantId = year.getSchool().getTenantId();
        }
        if (tenantId == null) {
            throw new IllegalStateException("tenant_id manquant pour l'année scolaire ; complétez l'école ou l'année.");
        }
        schoolClass.setTenantId(tenantId);
        schoolClass.setYear(year);
        schoolClass.setLevel(classLevelRepository.getReferenceById(schoolClass.getLevel().getId()));
        if (schoolClass.getCapacity() == null || schoolClass.getCapacity() < 1) {
            schoolClass.setCapacity(40);
        }
        if (schoolClass.getPeriodType() == null) {
            schoolClass.setPeriodType(PeriodType.TRIMESTER);
        }

        SchoolClass saved = repository.save(schoolClass);
        if (isNew) {
            gradingPeriodService.createForNewClass(saved, year);
        }
        return saved;
    }

    /**
     * Met à jour le nom, le niveau et la capacité. L’année et le type de période restent inchangés.
     */
    @Transactional
    public SchoolClass update(Long classId, UpdateSchoolClassRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Corps de requête obligatoire.");
        }
        String name = request.name() != null ? request.name().trim() : "";
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Le nom de la classe est obligatoire.");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("Le nom de la classe ne peut pas dépasser 50 caractères.");
        }
        if (request.levelId() == null) {
            throw new IllegalArgumentException("levelId est obligatoire.");
        }
        Integer capacity = request.capacity();
        if (capacity == null || capacity < 1 || capacity > 200) {
            throw new IllegalArgumentException("La capacité doit être entre 1 et 200.");
        }

        SchoolClass sc = repository.findByIdWithYearAndSchool(classId)
            .orElseThrow(() -> new IllegalArgumentException("Classe introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(sc.getYear().getSchool().getId());

        ClassLevel level = classLevelRepository.findById(request.levelId())
            .orElseThrow(() -> new IllegalArgumentException("Niveau introuvable."));

        long enrolled = studentRepository.countBySchoolClass_Id(classId);
        if (capacity < enrolled) {
            throw new IllegalArgumentException(
                "La capacité (" + capacity + ") ne peut pas être inférieure à l’effectif actuel ("
                    + enrolled + ")."
            );
        }

        Long yearId = sc.getYear().getId();
        if (repository.existsByYear_IdAndLevel_IdAndNameAndIdNot(yearId, level.getId(), name, classId)) {
            throw new IllegalStateException(
                "Une classe avec ce nom existe déjà pour ce niveau sur l’année scolaire."
            );
        }

        sc.setName(name);
        sc.setLevel(level);
        sc.setCapacity(capacity);
        return repository.save(sc);
    }

    public Optional<SchoolClass> findById(Long id) {
        return repository.findByIdWithYearAndSchool(id)
            .map(sc -> {
                schoolService.assertCurrentUserCanAccessSchool(sc.getYear().getSchool().getId());
                return sc;
            })
            .filter(this::isSchoolClassVisibleToTeacherIfApplicable);
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> findByYear(Long yearId) {
        SchoolYear year = schoolYearRepository.findByIdWithSchool(yearId)
            .orElseThrow(() -> new IllegalArgumentException("Année scolaire introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(year.getSchool().getId());
        return sortClasses(filterClassesForTeacher(repository.findByYear_Id(yearId)));
    }

    /**
     * Classes de l’année scolaire <strong>active</strong> pour l’établissement {@code schoolId}
     * (école sélectionnée côté client). Contrôle d’accès tenant aligné sur {@link SchoolService#listForAuthenticatedUser()}.
     */
    @Transactional(readOnly = true)
    public List<SchoolClass> listForActiveSchoolYear(Long schoolId) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return sortClasses(filterClassesForTeacher(repository.findByYear_School_IdAndYear_ActiveTrue(schoolId)));
    }

    @Transactional(readOnly = true)
    public List<SchoolClassOverviewResponse> listOverviewForActiveSchoolYear(Long schoolId) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        List<SchoolClass> classes = sortClasses(filterClassesForTeacher(
            repository.findByYear_School_IdAndYear_ActiveTrue(schoolId)
        ));
        if (classes.isEmpty()) {
            return List.of();
        }
        List<Long> ids = classes.stream().map(SchoolClass::getId).toList();
        Map<Long, Long> studentsByClass = toCountMap(studentRepository.countBySchoolClassIds(ids));
        Map<Long, Long> subjectsByClass = toCountMap(classSubjectRepository.countBySchoolClassIds(ids));
        return classes.stream()
            .map(sc -> toOverview(
                sc,
                studentsByClass.getOrDefault(sc.getId(), 0L),
                subjectsByClass.getOrDefault(sc.getId(), 0L)
            ))
            .toList();
    }

    private static List<SchoolClass> sortClasses(List<SchoolClass> classes) {
        return classes.stream().sorted(ClassLevelOrdering.schoolClassComparator()).toList();
    }

    private List<SchoolClass> filterClassesForTeacher(List<SchoolClass> classes) {
        Optional<Set<Long>> allowed = teacherTimetableAccessService.allowedSchoolClassIdsForCurrentUser();
        if (allowed.isEmpty()) {
            return classes;
        }
        Set<Long> ids = allowed.get();
        if (ids.isEmpty()) {
            return List.of();
        }
        return classes.stream()
            .filter(c -> c.getId() != null && ids.contains(c.getId()))
            .collect(Collectors.toList());
    }

    private boolean isSchoolClassVisibleToTeacherIfApplicable(SchoolClass sc) {
        Optional<Set<Long>> allowed = teacherTimetableAccessService.allowedSchoolClassIdsForCurrentUser();
        if (allowed.isEmpty()) {
            return true;
        }
        Set<Long> ids = allowed.get();
        if (ids.isEmpty()) {
            return false;
        }
        return sc.getId() != null && ids.contains(sc.getId());
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> out = new HashMap<>();
        if (rows == null) {
            return out;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            out.put((Long) row[0], (Long) row[1]);
        }
        return out;
    }

    private static SchoolClassOverviewResponse toOverview(SchoolClass sc, long enrolled, long subjectCount) {
        SchoolYear y = sc.getYear();
        ClassLevel lv = sc.getLevel();
        SchoolClassOverviewResponse.ClassLevelGroupRef groupRef = null;
        SchoolClassOverviewResponse.ClassLevelRef levelRef = null;
        if (lv != null) {
            ClassLevelGroup g = lv.getGroup();
            if (g != null) {
                groupRef = new SchoolClassOverviewResponse.ClassLevelGroupRef(g.getId(), g.getCode(), g.getName());
            }
            levelRef = new SchoolClassOverviewResponse.ClassLevelRef(lv.getId(), lv.getCode(), lv.getName(), groupRef);
        }
        SchoolClassOverviewResponse.SchoolYearRef yearRef = null;
        if (y != null) {
            yearRef = new SchoolClassOverviewResponse.SchoolYearRef(y.getId(), y.getLabel());
        }
        Integer cap = sc.getCapacity() != null ? sc.getCapacity() : 40;
        PeriodType pt = sc.getPeriodType() != null ? sc.getPeriodType() : PeriodType.TRIMESTER;
        return new SchoolClassOverviewResponse(
            sc.getId(),
            sc.getName(),
            cap,
            pt,
            yearRef,
            levelRef,
            enrolled,
            subjectCount
        );
    }

    /**
     * Suppression d’une classe uniquement si aucun élève n’y est rattaché.
     * Les matières, EDT, périodes et notes de la classe sont effacés en cascade.
     */
    @Transactional
    public void deleteIfEmpty(Long classId) {
        SchoolClass sc = repository.findByIdWithYearAndSchool(classId)
            .orElseThrow(() -> new IllegalArgumentException("Classe introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(sc.getYear().getSchool().getId());
        if (studentRepository.existsBySchoolClass_Id(classId)
            || studentRepository.countBySchoolClass_Id(classId) > 0) {
            throw new IllegalStateException(
                "Impossible de supprimer une classe qui contient encore des élèves. "
                    + "Transférez ou désaffectez les élèves d’abord."
            );
        }
        repository.delete(sc);
    }
}

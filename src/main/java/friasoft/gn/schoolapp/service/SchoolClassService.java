package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.response.SchoolClassOverviewResponse;
import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.repository.IClassLevelRepository;
import friasoft.gn.schoolapp.repository.IClassSubjectRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.ISchoolYearRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SchoolClassService {

    private final ISchoolClassRepository repository;
    private final ISchoolYearRepository schoolYearRepository;
    private final IClassLevelRepository classLevelRepository;
    private final IStudentRepository studentRepository;
    private final IClassSubjectRepository classSubjectRepository;
    private final SchoolService schoolService;

    @Transactional
    public SchoolClass save(SchoolClass schoolClass) {
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

        return repository.save(schoolClass);
    }

    public Optional<SchoolClass> findById(Long id) {
        return repository.findByIdWithYearAndSchool(id).map(sc -> {
            schoolService.assertCurrentUserCanAccessSchool(sc.getYear().getSchool().getId());
            return sc;
        });
    }

    public List<SchoolClass> findByYear(Long yearId) {
        SchoolYear year = schoolYearRepository.findByIdWithSchool(yearId)
            .orElseThrow(() -> new IllegalArgumentException("Année scolaire introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(year.getSchool().getId());
        return repository.findByYear_Id(yearId);
    }

    /**
     * Classes de l’année scolaire <strong>active</strong> pour l’établissement {@code schoolId}
     * (école sélectionnée côté client). Contrôle d’accès tenant aligné sur {@link SchoolService#listForAuthenticatedUser()}.
     */
    @Transactional(readOnly = true)
    public List<SchoolClass> listForActiveSchoolYear(Long schoolId) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return repository.findByYear_School_IdAndYear_ActiveTrue(schoolId);
    }

    @Transactional(readOnly = true)
    public List<SchoolClassOverviewResponse> listOverviewForActiveSchoolYear(Long schoolId) {
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        List<SchoolClass> classes = repository.findByYear_School_IdAndYear_ActiveTrue(schoolId);
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
        return new SchoolClassOverviewResponse(
            sc.getId(),
            sc.getName(),
            cap,
            yearRef,
            levelRef,
            enrolled,
            subjectCount
        );
    }
}

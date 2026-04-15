package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.repository.IClassLevelRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.ISchoolYearRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SchoolClassService {

    private final ISchoolClassRepository repository;
    private final ISchoolYearRepository schoolYearRepository;
    private final IClassLevelRepository classLevelRepository;
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

        return repository.save(schoolClass);
    }

    public Optional<SchoolClass> findById(Long id) {
        return repository.findById(id);
    }

    public List<SchoolClass> findByYear(Long yearId) {
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
}

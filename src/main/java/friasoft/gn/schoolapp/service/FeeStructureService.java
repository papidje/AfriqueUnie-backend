package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.FeeStructureDtos.FeeStructureRequest;
import friasoft.gn.schoolapp.dto.FeeStructureDtos.FeeStructureResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.entity.school.FeeStructure;
import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.repository.IClassLevelRepository;
import friasoft.gn.schoolapp.repository.IFeeStructureRepository;
import friasoft.gn.schoolapp.repository.ISchoolYearRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class FeeStructureService {

    private final IFeeStructureRepository feeStructureRepository;
    private final IClassLevelRepository classLevelRepository;
    private final ISchoolYearRepository schoolYearRepository;
    private final SchoolService schoolService;

    @Transactional(readOnly = true)
    public List<FeeStructureResponse> listBySchoolYear(Long schoolYearId) {
        SchoolYear year = loadYearAndAssertAccess(schoolYearId);
        return feeStructureRepository.findAllBySchoolYearIdWithRefs(year.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public FeeStructureResponse getById(Long id) {
        FeeStructure fs = feeStructureRepository.findByIdWithRefs(id)
            .orElseThrow(() -> new IllegalArgumentException("Structure tarifaire introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(fs.getSchoolYear().getSchool().getId());
        return toResponse(fs);
    }

    @Transactional
    public FeeStructureResponse create(FeeStructureRequest request) {
        validateRequest(request);
        SchoolYear year = loadYearAndAssertAccess(request.schoolYearId());
        ClassLevel level = loadClassLevel(request.classLevelId());

        if (feeStructureRepository.existsByClassLevel_IdAndSchoolYear_Id(level.getId(), year.getId())) {
            throw new IllegalArgumentException("Une structure tarifaire existe déjà pour ce niveau et cette année scolaire.");
        }

        FeeStructure fs = new FeeStructure();
        fs.setSchoolYear(year);
        fs.setClassLevel(level);
        fs.setRegistrationFee(request.registrationFee());
        fs.setReRegistrationFee(request.reRegistrationFee());
        fs.setMonthlyTuitionFee(request.monthlyTuitionFee());
        fs.setSuppliesFee(request.suppliesFee());
        fs.setSuppliesColumnEnabled(Boolean.TRUE.equals(request.suppliesColumnEnabled()));
        fs.setCurrency(normalizeCurrency(request.currency()));
        fs.setTenantId(resolveTenantId(year));

        return toResponse(feeStructureRepository.save(fs));
    }

    @Transactional
    public FeeStructureResponse update(Long id, FeeStructureRequest request) {
        validateRequest(request);
        FeeStructure existing = feeStructureRepository.findByIdWithRefs(id)
            .orElseThrow(() -> new IllegalArgumentException("Structure tarifaire introuvable."));

        schoolService.assertCurrentUserCanAccessSchool(existing.getSchoolYear().getSchool().getId());
        SchoolYear year = loadYearAndAssertAccess(request.schoolYearId());
        ClassLevel level = loadClassLevel(request.classLevelId());

        boolean changingKey = !existing.getSchoolYear().getId().equals(year.getId())
            || !existing.getClassLevel().getId().equals(level.getId());
        if (changingKey && feeStructureRepository.existsByClassLevel_IdAndSchoolYear_Id(level.getId(), year.getId())) {
            throw new IllegalArgumentException("Une structure tarifaire existe déjà pour ce niveau et cette année scolaire.");
        }

        existing.setSchoolYear(year);
        existing.setClassLevel(level);
        existing.setRegistrationFee(request.registrationFee());
        existing.setReRegistrationFee(request.reRegistrationFee());
        existing.setMonthlyTuitionFee(request.monthlyTuitionFee());
        existing.setSuppliesFee(request.suppliesFee());
        existing.setSuppliesColumnEnabled(Boolean.TRUE.equals(request.suppliesColumnEnabled()));
        existing.setCurrency(normalizeCurrency(request.currency()));
        existing.setTenantId(resolveTenantId(year));

        return toResponse(feeStructureRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        FeeStructure existing = feeStructureRepository.findByIdWithRefs(id)
            .orElseThrow(() -> new IllegalArgumentException("Structure tarifaire introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(existing.getSchoolYear().getSchool().getId());
        feeStructureRepository.delete(existing);
    }

    private SchoolYear loadYearAndAssertAccess(Long schoolYearId) {
        SchoolYear year = schoolYearRepository.findByIdWithSchool(schoolYearId)
            .orElseThrow(() -> new IllegalArgumentException("Année scolaire introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(year.getSchool().getId());
        return year;
    }

    private ClassLevel loadClassLevel(Long classLevelId) {
        return classLevelRepository.findById(classLevelId)
            .orElseThrow(() -> new IllegalArgumentException("Niveau de classe introuvable."));
    }

    private void validateRequest(FeeStructureRequest request) {
        if (request.classLevelId() == null) {
            throw new IllegalArgumentException("classLevelId est obligatoire.");
        }
        if (request.schoolYearId() == null) {
            throw new IllegalArgumentException("schoolYearId est obligatoire.");
        }
        if (request.registrationFee() == null || request.registrationFee() < 0) {
            throw new IllegalArgumentException("registrationFee doit être >= 0.");
        }
        if (request.reRegistrationFee() == null || request.reRegistrationFee() < 0) {
            throw new IllegalArgumentException("reRegistrationFee doit être >= 0.");
        }
        if (request.monthlyTuitionFee() == null || request.monthlyTuitionFee() < 0) {
            throw new IllegalArgumentException("monthlyTuitionFee doit être >= 0.");
        }
        if (request.suppliesFee() == null || request.suppliesFee() < 0) {
            throw new IllegalArgumentException("suppliesFee doit être >= 0.");
        }
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "GNF";
        }
        return currency.trim().toUpperCase();
    }

    private Long resolveTenantId(SchoolYear year) {
        User current = currentUserOrNull();
        if (current != null && current.getTenantId() != null) {
            return current.getTenantId();
        }
        return year.getTenantId();
    }

    private static User currentUserOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }

    private FeeStructureResponse toResponse(FeeStructure fs) {
        ClassLevel level = fs.getClassLevel();
        SchoolYear year = fs.getSchoolYear();
        return new FeeStructureResponse(
            fs.getId(),
            fs.getTenantId(),
            level.getId(),
            level.getCode(),
            level.getName(),
            year.getId(),
            year.getLabel(),
            fs.getRegistrationFee(),
            fs.getReRegistrationFee(),
            fs.getMonthlyTuitionFee(),
            fs.getSuppliesFee(),
            fs.getSuppliesColumnEnabled(),
            fs.getCurrency()
        );
    }
}

package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.FeeStructureDtos.FeeStructureRequest;
import friasoft.gn.schoolapp.dto.FeeStructureDtos.FeeStructureResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.entity.school.FeeStructure;
import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.repository.IClassLevelRepository;
import friasoft.gn.schoolapp.repository.IFeeStructureRepository;
import friasoft.gn.schoolapp.repository.IPaymentRepository;
import friasoft.gn.schoolapp.repository.ISchoolYearRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class FeeStructureService {

    private final IFeeStructureRepository feeStructureRepository;
    private final IClassLevelRepository classLevelRepository;
    private final ISchoolYearRepository schoolYearRepository;
    private final IPaymentRepository paymentRepository;
    private final SchoolService schoolService;

    @Transactional(readOnly = true)
    public List<FeeStructureResponse> listBySchoolYear(Long schoolYearId) {
        SchoolYear year = loadYearAndAssertAccess(schoolYearId);
        Set<Long> lockedLevels = new HashSet<>(
            paymentRepository.findClassLevelIdsWithPaymentsForSchoolYear(year.getId())
        );
        return feeStructureRepository.findAllBySchoolYearIdWithRefs(year.getId()).stream()
            .map(fs -> toResponse(fs, lockedLevels.contains(fs.getClassLevel().getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public FeeStructureResponse getById(Long id) {
        FeeStructure fs = feeStructureRepository.findByIdWithRefs(id)
            .orElseThrow(() -> new IllegalArgumentException("Structure tarifaire introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(fs.getSchoolYear().getSchool().getId());
        return toResponse(fs, isLocked(fs));
    }

    @Transactional
    public FeeStructureResponse create(FeeStructureRequest request) {
        validateRequest(request);
        SchoolYear year = loadYearAndAssertAccess(request.schoolYearId());
        ClassLevel level = loadClassLevel(request.classLevelId());

        if (feeStructureRepository.existsByClassLevel_IdAndSchoolYear_Id(level.getId(), year.getId())) {
            throw new IllegalArgumentException("Une structure tarifaire existe déjà pour ce niveau et cette année scolaire.");
        }
        if (paymentRepository.existsPaymentForClassLevelAndSchoolYear(level.getId(), year.getId())) {
            throw new IllegalArgumentException(
                "Impossible de créer ce barème : des encaissements ont déjà été enregistrés pour ce niveau sur cette année scolaire."
            );
        }

        FeeStructure fs = new FeeStructure();
        fs.setSchoolYear(year);
        fs.setClassLevel(level);
        fs.setRegistrationFee(request.registrationFee());
        fs.setReRegistrationFee(request.reRegistrationFee());
        applyTuition(fs, request);
        fs.setSuppliesFee(request.suppliesFee());
        fs.setSuppliesColumnEnabled(Boolean.TRUE.equals(request.suppliesColumnEnabled()));
        fs.setCurrency(normalizeCurrency(request.currency()));
        fs.setTenantId(resolveTenantId(year));

        FeeStructure saved = feeStructureRepository.save(fs);
        return toResponse(saved, isLocked(saved));
    }

    @Transactional
    public FeeStructureResponse update(Long id, FeeStructureRequest request) {
        validateRequest(request);
        FeeStructure existing = feeStructureRepository.findByIdWithRefs(id)
            .orElseThrow(() -> new IllegalArgumentException("Structure tarifaire introuvable."));

        schoolService.assertCurrentUserCanAccessSchool(existing.getSchoolYear().getSchool().getId());
        assertNotLocked(existing);

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
        applyTuition(existing, request);
        existing.setSuppliesFee(request.suppliesFee());
        existing.setSuppliesColumnEnabled(Boolean.TRUE.equals(request.suppliesColumnEnabled()));
        existing.setCurrency(normalizeCurrency(request.currency()));
        existing.setTenantId(resolveTenantId(year));

        FeeStructure saved = feeStructureRepository.save(existing);
        return toResponse(saved, isLocked(saved));
    }

    @Transactional
    public void delete(Long id) {
        FeeStructure existing = feeStructureRepository.findByIdWithRefs(id)
            .orElseThrow(() -> new IllegalArgumentException("Structure tarifaire introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(existing.getSchoolYear().getSchool().getId());
        assertNotLocked(existing);
        feeStructureRepository.delete(existing);
    }

    private void assertNotLocked(FeeStructure fs) {
        if (isLocked(fs)) {
            throw new IllegalArgumentException(
                "Ce barème est verrouillé : des encaissements ont déjà été enregistrés pour ce niveau sur cette année scolaire."
            );
        }
    }

    private boolean isLocked(FeeStructure fs) {
        if (fs.getClassLevel() == null || fs.getSchoolYear() == null) {
            return false;
        }
        return paymentRepository.existsPaymentForClassLevelAndSchoolYear(
            fs.getClassLevel().getId(),
            fs.getSchoolYear().getId()
        );
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

    /**
     * Annuelle si {@code annualTuitionFee != null} (même à 0) — permet de restaurer le toggle.
     * Sinon mensuelle : {@code monthlyTuitionFee}, {@code annualTuitionFee} null.
     */
    private void applyTuition(FeeStructure fs, FeeStructureRequest request) {
        if (request.annualTuitionFee() != null) {
            if (request.annualTuitionFee() < 0) {
                throw new IllegalArgumentException("annualTuitionFee doit être >= 0.");
            }
            fs.setAnnualTuitionFee(request.annualTuitionFee());
            fs.setMonthlyTuitionFee(0d);
            return;
        }
        if (request.monthlyTuitionFee() == null || request.monthlyTuitionFee() < 0) {
            throw new IllegalArgumentException("monthlyTuitionFee doit être >= 0.");
        }
        fs.setMonthlyTuitionFee(request.monthlyTuitionFee());
        fs.setAnnualTuitionFee(null);
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
        if (request.annualTuitionFee() != null) {
            if (request.annualTuitionFee() < 0) {
                throw new IllegalArgumentException("annualTuitionFee doit être >= 0.");
            }
        } else if (request.monthlyTuitionFee() == null || request.monthlyTuitionFee() < 0) {
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

    private FeeStructureResponse toResponse(FeeStructure fs, boolean locked) {
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
            fs.getAnnualTuitionFee(),
            fs.getSuppliesFee(),
            fs.getSuppliesColumnEnabled(),
            fs.getCurrency(),
            locked
        );
    }
}

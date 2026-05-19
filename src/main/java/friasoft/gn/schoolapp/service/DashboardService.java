package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.response.DashboardResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.IClassSubjectRepository;
import friasoft.gn.schoolapp.repository.IPaymentRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import friasoft.gn.schoolapp.security.SecurityAuthorityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_ENROLLMENTS_LIMIT = 10;

    private final IStudentRepository studentRepository;
    private final IPaymentRepository paymentRepository;
    private final ISchoolClassRepository schoolClassRepository;
    private final IClassSubjectRepository classSubjectRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolService schoolService;
    private final UserRepository userRepository;

    public DashboardResponse getMockSummary() {
        List<DashboardResponse.RecentEnrollmentResponse> recent = List.of(
            new DashboardResponse.RecentEnrollmentResponse(1L, "Aminata Camara", "6e A", LocalDate.now().minusDays(1)),
            new DashboardResponse.RecentEnrollmentResponse(2L, "Mamadou Barry", "5e C", LocalDate.now().minusDays(2)),
            new DashboardResponse.RecentEnrollmentResponse(3L, "Fatou Diallo", "Terminale D", LocalDate.now().minusDays(3)),
            new DashboardResponse.RecentEnrollmentResponse(4L, "Ibrahima Bah", "4e B", LocalDate.now().minusDays(4)),
            new DashboardResponse.RecentEnrollmentResponse(5L, "Kadiatou Sylla", "CM2", LocalDate.now().minusDays(5)),
            new DashboardResponse.RecentEnrollmentResponse(6L, "Ousmane Keita", "3e A", LocalDate.now().minusDays(6)),
            new DashboardResponse.RecentEnrollmentResponse(7L, "Mariam Sow", "2nde C", LocalDate.now().minusDays(7)),
            new DashboardResponse.RecentEnrollmentResponse(8L, "Alpha Diop", "CP", LocalDate.now().minusDays(8)),
            new DashboardResponse.RecentEnrollmentResponse(9L, "Hawa Cissé", "1ère S", LocalDate.now().minusDays(9)),
            new DashboardResponse.RecentEnrollmentResponse(10L, "Cheikh Touré", "CE1", LocalDate.now().minusDays(10))
        );
        return new DashboardResponse(
            312L,
            400L,
            12L,
            84L,
            BigDecimal.valueOf(18_450_000L),
            BigDecimal.valueOf(142_000_000L),
            recent
        );
    }

    @Transactional(readOnly = true)
    public DashboardResponse getSummary(Authentication authentication, Long requestedSchoolId) {
        if (!(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        long schoolId = resolveDashboardSchoolId(authentication, user, requestedSchoolId);
        schoolService.assertCurrentUserCanAccessSchool(schoolId);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        long studentsEnrolled = studentRepository.countStudentsActiveSchoolYear(schoolId);
        long totalCapacity = schoolClassRepository.sumCapacityActiveSchoolYear(schoolId);
        long classesCount = schoolClassRepository.countActiveSchoolYearClasses(schoolId);
        long taughtSubjectsCount = classSubjectRepository.countForActiveSchoolYear(schoolId);

        Double collectedMonth = paymentRepository.sumCollectedAmountBetweenForSchool(schoolId, monthStart, monthEnd);
        BigDecimal monthlyTuitionCollected = BigDecimal.valueOf(collectedMonth != null ? collectedMonth : 0d);

        Double collectedYear = paymentRepository.sumCollectedForActiveSchoolYear(schoolId);
        BigDecimal schoolYearTuitionCollected = BigDecimal.valueOf(collectedYear != null ? collectedYear : 0d);

        List<DashboardResponse.RecentEnrollmentResponse> recentEnrollmentResponses =
            studentRepository.findRecentWithClassForSchool(schoolId, PageRequest.of(0, RECENT_ENROLLMENTS_LIMIT)).stream()
                .map(student -> new DashboardResponse.RecentEnrollmentResponse(
                    student.getId(),
                    (student.getFirstName() + " " + student.getLastName()).trim(),
                    student.getSchoolClass() != null ? student.getSchoolClass().getName() : "—",
                    student.getCreatedAt() != null ? student.getCreatedAt().toLocalDate() : LocalDate.now()
                ))
                .toList();

        return new DashboardResponse(
            studentsEnrolled,
            totalCapacity,
            classesCount,
            taughtSubjectsCount,
            monthlyTuitionCollected,
            schoolYearTuitionCollected,
            recentEnrollmentResponses
        );
    }

    /**
     * Détermine l’établissement pour les indicateurs (pas l’agrégat tenant entier).
     */
    private long resolveDashboardSchoolId(Authentication authentication, User user, Long requestedSchoolId) {
        final boolean singleSchoolStaffLike = isSingleSchoolStaffLike(authentication);

        if (requestedSchoolId != null) {
            // L’accès effectif est contrôlé par assertCurrentUserCanAccessSchool (affiliations / plateforme).
            // Ne pas comparer à users.school_id : profils multi-écoles ou invitations cross-tenant ont une école
            // JWT différente de la colonne legacy users.school_id.
            return requestedSchoolId;
        }
        if (SecurityAuthorityUtils.hasAuthority(SecurityAuthorityUtils.ROLE_DIRECTOR)) {
            Long directorSchoolId = userRepository.findSchoolIdByUserId(user.getId()).orElse(null);
            if (directorSchoolId != null) {
                return directorSchoolId;
            }
        }
        if (singleSchoolStaffLike) {
            Long staffSchoolId = userRepository.findSchoolIdByUserId(user.getId()).orElse(null);
            if (staffSchoolId != null) {
                return staffSchoolId;
            }
        }
        if (SecurityAuthorityUtils.hasAuthority(SecurityAuthorityUtils.ROLE_ADMIN_ECOLE)
            && user.getOrganizationTenantId() != null) {
            List<School> schools = schoolRepository.findByTenantIdOrderByIdAsc(user.getOrganizationTenantId());
            if (schools.size() == 1) {
                return schools.get(0).getId();
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paramètre schoolId obligatoire.");
    }

    private boolean isSingleSchoolStaffLike(Authentication authentication) {
        if (authentication == null
            || SecurityAuthorityUtils.hasAuthority(SecurityAuthorityUtils.ROLE_SUPER_ADMIN)
            || SecurityAuthorityUtils.hasAuthority(SecurityAuthorityUtils.ROLE_ADMIN_ECOLE)
            || SecurityAuthorityUtils.hasAuthority(SecurityAuthorityUtils.ROLE_DIRECTOR)) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(a -> {
            String ga = a.getAuthority();
            return "ROLE_TEACHER".equals(ga) || "ROLE_STAFF".equals(ga);
        });
    }
}

package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.response.DashboardResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.IPaymentRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
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

    private final IStudentRepository studentRepository;
    private final IPaymentRepository paymentRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolService schoolService;
    private final UserRepository userRepository;

    public DashboardResponse getMockSummary() {
        return new DashboardResponse(
            428,
            BigDecimal.valueOf(18_450_000L),
            List.of(
                new DashboardResponse.RecentEnrollmentResponse(1L, "Aminata Camara", "6e A", LocalDate.now().minusDays(1)),
                new DashboardResponse.RecentEnrollmentResponse(2L, "Mamadou Barry", "5e C", LocalDate.now().minusDays(2)),
                new DashboardResponse.RecentEnrollmentResponse(3L, "Fatou Diallo", "Terminale D", LocalDate.now().minusDays(3)),
                new DashboardResponse.RecentEnrollmentResponse(4L, "Ibrahima Bah", "4e B", LocalDate.now().minusDays(4)),
                new DashboardResponse.RecentEnrollmentResponse(5L, "Kadiatou Sylla", "CM2", LocalDate.now().minusDays(5))
            )
        );
    }

    @Transactional(readOnly = true)
    public DashboardResponse getSummary(Authentication authentication, Long requestedSchoolId) {
        if (!(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        long schoolId = resolveDashboardSchoolId(user, requestedSchoolId);
        schoolService.assertCurrentUserCanAccessSchool(schoolId);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        long studentsCount = studentRepository.countBySchoolId(schoolId);
        Double collected = paymentRepository.sumCollectedAmountBetweenForSchool(schoolId, monthStart, monthEnd);
        BigDecimal monthlyTuitionCollected = BigDecimal.valueOf(collected != null ? collected : 0d);

        List<DashboardResponse.RecentEnrollmentResponse> recentEnrollmentResponses =
            studentRepository.findRecentWithClassForSchool(schoolId, PageRequest.of(0, 5)).stream()
                .map(student -> new DashboardResponse.RecentEnrollmentResponse(
                    student.getId(),
                    (student.getFirstName() + " " + student.getLastName()).trim(),
                    student.getSchoolClass() != null ? student.getSchoolClass().getName() : "—",
                    student.getCreatedAt() != null ? student.getCreatedAt().toLocalDate() : LocalDate.now()
                ))
                .toList();

        return new DashboardResponse(studentsCount, monthlyTuitionCollected, recentEnrollmentResponses);
    }

    /**
     * Détermine l’établissement pour les indicateurs (pas l’agrégat tenant entier).
     */
    private long resolveDashboardSchoolId(User user, Long requestedSchoolId) {
        if (user.getRole() == User.UserRole.SUPER_ADMIN) {
            if (requestedSchoolId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paramètre schoolId obligatoire.");
            }
            return requestedSchoolId;
        }
        if (requestedSchoolId != null) {
            Long assigned = userRepository.findAssignedSchoolIdByUserId(user.getId()).orElse(null);
            if (assigned != null && !assigned.equals(requestedSchoolId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Établissement non autorisé pour ce compte.");
            }
            return requestedSchoolId;
        }
        Long staffSchoolId = userRepository.findAssignedSchoolIdByUserId(user.getId()).orElse(null);
        if (staffSchoolId != null) {
            return staffSchoolId;
        }
        if (user.getRole() == User.UserRole.ADMIN_ECOLE && user.getTenantId() != null) {
            List<School> schools = schoolRepository.findByTenantIdOrderByIdAsc(user.getTenantId());
            if (schools.size() == 1) {
                return schools.get(0).getId();
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paramètre schoolId obligatoire.");
    }
}

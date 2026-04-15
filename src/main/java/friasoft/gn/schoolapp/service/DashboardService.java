package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.response.DashboardResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.Enrollment;
import friasoft.gn.schoolapp.repository.IEnrollmentRepository;
import friasoft.gn.schoolapp.repository.IPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final IEnrollmentRepository enrollmentRepository;
    private final IPaymentRepository paymentRepository;

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
    public DashboardResponse getSummary(Authentication authentication) {
        Long schoolId = extractSchoolId(authentication);
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        long studentsCount = schoolId == null
            ? enrollmentRepository.countDistinctStudents()
            : enrollmentRepository.countDistinctStudentsBySchoolId(schoolId);

        BigDecimal monthlyTuitionCollected = schoolId == null
            ? paymentRepository.sumCollectedAmountBetween(monthStart, monthEnd)
            : paymentRepository.sumCollectedAmountBySchoolBetween(schoolId, monthStart, monthEnd);

        List<Enrollment> recentEnrollments = schoolId == null
            ? enrollmentRepository.findAllByOrderByEnrolledOnDescIdDesc(PageRequest.of(0, 5))
            : enrollmentRepository.findByClassRef_Year_School_IdOrderByEnrolledOnDescIdDesc(schoolId, PageRequest.of(0, 5));

        List<DashboardResponse.RecentEnrollmentResponse> recentEnrollmentResponses = recentEnrollments.stream()
            .map(enrollment -> new DashboardResponse.RecentEnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getFirstName() + " " + enrollment.getStudent().getLastName(),
                enrollment.getClassRef().getName(),
                enrollment.getEnrolledOn()
            ))
            .toList();

        return new DashboardResponse(studentsCount, monthlyTuitionCollected, recentEnrollmentResponses);
    }

    private Long extractSchoolId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        if (user.getSchool() == null) {
            return null;
        }
        return user.getSchool().getId();
    }
}

package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.response.DashboardResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.FeeStructure;
import friasoft.gn.schoolapp.entity.school.Payment;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.entity.school.StudentAccount;
import friasoft.gn.schoolapp.repository.IFeeStructureRepository;
import friasoft.gn.schoolapp.repository.IPaymentRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.IStudentAccountRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import friasoft.gn.schoolapp.security.SecurityAuthorityUtils;
import friasoft.gn.schoolapp.service.finance.TuitionMonthDues;
import friasoft.gn.schoolapp.util.ClassLevelOrdering;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final double EPSILON = 1e-6;

    private final IStudentRepository studentRepository;
    private final IPaymentRepository paymentRepository;
    private final ISchoolClassRepository schoolClassRepository;
    private final IFeeStructureRepository feeStructureRepository;
    private final IStudentAccountRepository studentAccountRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolService schoolService;
    private final UserRepository userRepository;

    public DashboardResponse getMockSummary() {
        List<DashboardResponse.ClassFillItem> fill = List.of(
            new DashboardResponse.ClassFillItem(1L, "Garderie", "GAR", 22L, 40L),
            new DashboardResponse.ClassFillItem(2L, "Petite Section", "PS", 28L, 40L),
            new DashboardResponse.ClassFillItem(3L, "CP1", "CP1", 30L, 40L),
            new DashboardResponse.ClassFillItem(4L, "CM2", "CM2", 25L, 40L)
        );
        List<DashboardResponse.ClassPaymentStatusItem> payments = List.of(
            new DashboardResponse.ClassPaymentStatusItem(1L, "Garderie", "GAR", 18L, 4L),
            new DashboardResponse.ClassPaymentStatusItem(2L, "Petite Section", "PS", 20L, 8L),
            new DashboardResponse.ClassPaymentStatusItem(3L, "CP1", "CP1", 25L, 5L),
            new DashboardResponse.ClassPaymentStatusItem(4L, "CM2", "CM2", 25L, 0L)
        );
        return new DashboardResponse(
            312L,
            400L,
            12L,
            BigDecimal.valueOf(142_000_000L),
            fill,
            payments
        );
    }

    @Transactional(readOnly = true)
    public DashboardResponse getSummary(Authentication authentication, Long requestedSchoolId) {
        if (!(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        long schoolId = resolveDashboardSchoolId(authentication, user, requestedSchoolId);
        schoolService.assertCurrentUserCanAccessSchool(schoolId);

        long studentsEnrolled = studentRepository.countStudentsActiveSchoolYear(schoolId);
        long totalCapacity = schoolClassRepository.sumCapacityActiveSchoolYear(schoolId);
        long classesCount = schoolClassRepository.countActiveSchoolYearClasses(schoolId);

        Double collectedYear = paymentRepository.sumCollectedForActiveSchoolYear(schoolId);
        BigDecimal schoolYearTuitionCollected = BigDecimal.valueOf(collectedYear != null ? collectedYear : 0d);

        List<SchoolClass> classes = schoolClassRepository.findByYear_School_IdAndYear_ActiveTrue(schoolId).stream()
            .sorted(ClassLevelOrdering.schoolClassComparator())
            .toList();

        List<DashboardResponse.ClassFillItem> classFill = buildClassFill(classes);
        List<DashboardResponse.ClassPaymentStatusItem> classPaymentStatus =
            buildClassPaymentStatus(classes, LocalDate.now());

        return new DashboardResponse(
            studentsEnrolled,
            totalCapacity,
            classesCount,
            schoolYearTuitionCollected,
            classFill,
            classPaymentStatus
        );
    }

    private List<DashboardResponse.ClassFillItem> buildClassFill(List<SchoolClass> classes) {
        if (classes.isEmpty()) {
            return List.of();
        }
        List<Long> ids = classes.stream().map(SchoolClass::getId).toList();
        Map<Long, Long> enrolledByClass = toCountMap(studentRepository.countBySchoolClassIds(ids));
        List<DashboardResponse.ClassFillItem> out = new ArrayList<>(classes.size());
        for (SchoolClass sc : classes) {
            long enrolled = enrolledByClass.getOrDefault(sc.getId(), 0L);
            long capacity = sc.getCapacity() != null ? sc.getCapacity().longValue() : 0L;
            out.add(new DashboardResponse.ClassFillItem(
                sc.getId(),
                sc.getName(),
                sc.getLevel() != null ? sc.getLevel().getCode() : null,
                enrolled,
                capacity
            ));
        }
        return out;
    }

    private List<DashboardResponse.ClassPaymentStatusItem> buildClassPaymentStatus(
        List<SchoolClass> classes,
        LocalDate asOf
    ) {
        if (classes.isEmpty()) {
            return List.of();
        }

        SchoolYear year = classes.get(0).getYear();
        LocalDate yearStart = year != null ? year.getStartDate() : null;
        Long yearId = year != null ? year.getId() : null;

        Map<Long, FeeStructure> feeByLevelId = new HashMap<>();
        if (yearId != null) {
            for (SchoolClass sc : classes) {
                if (sc.getLevel() == null || sc.getLevel().getId() == null) {
                    continue;
                }
                Long levelId = sc.getLevel().getId();
                if (feeByLevelId.containsKey(levelId)) {
                    continue;
                }
                feeStructureRepository.findByClassLevel_IdAndSchoolYear_Id(levelId, yearId)
                    .ifPresent(fs -> feeByLevelId.put(levelId, fs));
            }
        }

        List<DashboardResponse.ClassPaymentStatusItem> out = new ArrayList<>(classes.size());
        for (SchoolClass sc : classes) {
            List<Student> students = studentRepository.findBySchoolClass_Id(sc.getId());
            long upToDate = 0L;
            long late = 0L;
            if (!students.isEmpty() && yearId != null) {
                FeeStructure fee = sc.getLevel() != null ? feeByLevelId.get(sc.getLevel().getId()) : null;
                List<Long> studentIds = students.stream().map(Student::getId).toList();
                Map<Long, StudentAccount> accountByStudentId = studentAccountRepository
                    .findByStudent_IdInAndSchoolYear_Id(studentIds, yearId).stream()
                    .collect(Collectors.toMap(a -> a.getStudent().getId(), Function.identity(), (a, b) -> a));

                Set<Long> accountIds = accountByStudentId.values().stream()
                    .map(StudentAccount::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                Map<Long, PaymentSums> paidByAccountId = accountIds.isEmpty()
                    ? Map.of()
                    : groupPaymentSums(paymentRepository.findByStudentAccount_IdIn(accountIds));

                for (Student student : students) {
                    if (isLate(student, accountByStudentId, paidByAccountId, fee, yearStart, asOf)) {
                        late++;
                    } else {
                        upToDate++;
                    }
                }
            } else {
                upToDate = students.size();
            }
            out.add(new DashboardResponse.ClassPaymentStatusItem(
                sc.getId(),
                sc.getName(),
                sc.getLevel() != null ? sc.getLevel().getCode() : null,
                upToDate,
                late
            ));
        }
        return out;
    }

    /**
     * En retard si dû à date du jour (inscription + fournitures + mois échus) − payé &gt; 0.
     */
    private boolean isLate(
        Student student,
        Map<Long, StudentAccount> accountByStudentId,
        Map<Long, PaymentSums> paidByAccountId,
        FeeStructure feeStructure,
        LocalDate schoolYearStart,
        LocalDate asOf
    ) {
        if (feeStructure == null) {
            return false;
        }
        StudentAccount account = accountByStudentId.get(student.getId());
        PaymentSums sums = account == null
            ? PaymentSums.empty()
            : paidByAccountId.getOrDefault(account.getId(), PaymentSums.empty());

        double inscriptionPaid = sums.get(Payment.PaymentType.INSCRIPTION);
        double reInscriptionPaid = sums.get(Payment.PaymentType.REINSCRIPTION);
        boolean useReInscription = reInscriptionPaid > 0d;
        double insReinsPaid = useReInscription ? reInscriptionPaid : inscriptionPaid;
        double insReinsExpected = useReInscription
            ? nvl(feeStructure.getReRegistrationFee())
            : nvl(feeStructure.getRegistrationFee());
        double insReinsRemaining = Math.max(0d, insReinsExpected - insReinsPaid);

        double payablePercent = resolveTuitionPayablePercent(account);
        double[] tuitionDues = TuitionMonthDues.forFeeStructure(feeStructure, payablePercent);
        double tuitionDueAsOf = TuitionMonthDues.sumDueAsOf(tuitionDues, schoolYearStart, asOf);
        double tuitionPaid = sums.get(Payment.PaymentType.SCOLARITE);
        double tuitionRemainingAsOf = Math.max(0d, tuitionDueAsOf - tuitionPaid);

        boolean suppliesColumnEnabled = Boolean.TRUE.equals(feeStructure.getSuppliesColumnEnabled());
        double suppliesExpected = suppliesColumnEnabled ? nvl(feeStructure.getSuppliesFee()) : 0d;
        boolean hasPaidSupplies = Boolean.TRUE.equals(account != null ? account.getSuppliesPaid() : Boolean.FALSE)
            || sums.get(Payment.PaymentType.FOURNITURES) > 0d;
        double suppliesRemaining = (!suppliesColumnEnabled || hasPaidSupplies)
            ? 0d
            : Math.max(0d, suppliesExpected);

        double openAsOfToday = insReinsRemaining + suppliesRemaining + tuitionRemainingAsOf;
        return openAsOfToday > EPSILON;
    }

    private static double resolveTuitionPayablePercent(StudentAccount account) {
        if (account == null || account.getTuitionPayablePercent() == null) {
            return 100d;
        }
        return TuitionMonthDues.clampPercent(account.getTuitionPayablePercent());
    }

    private static Map<Long, PaymentSums> groupPaymentSums(List<Payment> payments) {
        Map<Long, PaymentSums> byAccount = new LinkedHashMap<>();
        for (Payment p : payments) {
            if (p.getStudentAccount() == null || p.getStudentAccount().getId() == null) {
                continue;
            }
            Long accountId = p.getStudentAccount().getId();
            byAccount.computeIfAbsent(accountId, ignored -> PaymentSums.empty())
                .add(p.getPaymentType(), nvl(p.getAmount()));
        }
        return byAccount;
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

    private static double nvl(Double value) {
        return value == null ? 0d : value;
    }

    private long resolveDashboardSchoolId(Authentication authentication, User user, Long requestedSchoolId) {
        final boolean singleSchoolStaffLike = isSingleSchoolStaffLike(authentication);

        if (requestedSchoolId != null) {
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

    private static final class PaymentSums {
        private final EnumMap<Payment.PaymentType, Double> byType = new EnumMap<>(Payment.PaymentType.class);

        static PaymentSums empty() {
            return new PaymentSums();
        }

        void add(Payment.PaymentType type, double amount) {
            if (type == null) {
                return;
            }
            byType.merge(type, amount, Double::sum);
        }

        double get(Payment.PaymentType type) {
            return byType.getOrDefault(type, 0d);
        }
    }
}

package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.StudentPaymentStatusDTO;
import friasoft.gn.schoolapp.dto.StudentPaymentInfoDTO;
import friasoft.gn.schoolapp.dto.FinancePaymentDtos.CreatePaymentRequest;
import friasoft.gn.schoolapp.dto.FinancePaymentDtos.CreatePaymentResponse;
import friasoft.gn.schoolapp.entity.school.FeeStructure;
import friasoft.gn.schoolapp.entity.school.Payment;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.entity.school.StudentAccount;
import friasoft.gn.schoolapp.repository.IFeeStructureRepository;
import friasoft.gn.schoolapp.repository.IPaymentRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.IStudentAccountRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class FinanceService {

    private static final List<String> MONTHS_OCT_TO_JUN = List.of("OCT", "NOV", "DEC", "JAN", "FEB", "MAR", "APR", "MAY", "JUN");
    private static final List<String> MONTHS_LABELS_OCT_TO_JUN = List.of("Octobre", "Novembre", "Décembre", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin");

    private final ISchoolClassRepository schoolClassRepository;
    private final IStudentRepository studentRepository;
    private final IStudentAccountRepository studentAccountRepository;
    private final IPaymentRepository paymentRepository;
    private final IFeeStructureRepository feeStructureRepository;
    private final SchoolService schoolService;

    @Transactional(readOnly = true)
    public List<StudentPaymentStatusDTO> getClassPaymentStatus(Long classId) {
        if (classId == null) {
            throw new IllegalArgumentException("classId obligatoire.");
        }

        SchoolClass schoolClass = schoolClassRepository.findByIdWithYearAndSchool(classId)
            .orElseThrow(() -> new IllegalArgumentException("Classe introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(schoolClass.getYear().getSchool().getId());

        FeeStructure feeStructure = feeStructureRepository
            .findByClassLevel_IdAndSchoolYear_Id(schoolClass.getLevel().getId(), schoolClass.getYear().getId())
            .orElseThrow(() -> new IllegalArgumentException("Aucune structure de frais trouvée pour ce niveau et cette année."));

        double registrationFee = nvl(feeStructure.getRegistrationFee());
        double monthlyFee = nvl(feeStructure.getMonthlyTuitionFee());
        double suppliesFee = nvl(feeStructure.getSuppliesFee());
        double tuitionExpected = monthlyFee * MONTHS_OCT_TO_JUN.size();
        double totalExpected = registrationFee + tuitionExpected + suppliesFee;

        List<Student> students = studentRepository.findBySchoolClass_Id(classId);
        if (students.isEmpty()) {
            return List.of();
        }

        List<Long> studentIds = students.stream().map(Student::getId).toList();
        Map<Long, StudentAccount> accountByStudentId = studentAccountRepository
            .findByStudent_IdInAndSchoolYear_Id(studentIds, schoolClass.getYear().getId()).stream()
            .collect(Collectors.toMap(a -> a.getStudent().getId(), Function.identity()));

        Set<Long> accountIds = accountByStudentId.values().stream().map(StudentAccount::getId).collect(Collectors.toSet());
        Map<Long, PaymentSums> paidByAccountId = accountIds.isEmpty()
            ? Map.of()
            : groupPaymentSumsByAccount(paymentRepository.findByStudentAccount_IdIn(accountIds));

        return students.stream()
            .map(student -> toStatus(student, accountByStudentId, paidByAccountId, feeStructure, monthlyFee, suppliesFee, tuitionExpected, totalExpected))
            .toList();
    }

    @Transactional(readOnly = true)
    public StudentPaymentInfoDTO getStudentPaymentInfo(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId obligatoire.");
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Élève introuvable."));
        if (student.getSchoolClass() == null || student.getSchoolClass().getYear() == null || student.getSchoolClass().getYear().getSchool() == null) {
            throw new IllegalArgumentException("Contexte classe/année de l'élève introuvable.");
        }

        SchoolClass schoolClass = student.getSchoolClass();
        schoolService.assertCurrentUserCanAccessSchool(schoolClass.getYear().getSchool().getId());

        FeeStructure feeStructure = feeStructureRepository
            .findByClassLevel_IdAndSchoolYear_Id(schoolClass.getLevel().getId(), schoolClass.getYear().getId())
            .orElseThrow(() -> new IllegalArgumentException("Aucune structure de frais trouvée pour ce niveau et cette année."));

        StudentAccount account = studentAccountRepository
            .findByStudent_IdAndSchoolYear_Id(studentId, schoolClass.getYear().getId())
            .orElseThrow(() -> new IllegalArgumentException("Compte élève introuvable pour l'année en cours."));

        PaymentSums sums = groupPaymentSumsByAccount(paymentRepository.findByStudentAccount_IdIn(List.of(account.getId())))
            .getOrDefault(account.getId(), PaymentSums.empty());

        double inscriptionPaid = sums.get(Payment.PaymentType.INSCRIPTION);
        double reInscriptionPaid = sums.get(Payment.PaymentType.REINSCRIPTION);
        boolean useReInscription = reInscriptionPaid > 0d;
        String insReinsType = useReInscription ? "REINSCRIPTION" : "INSCRIPTION";
        double insReinsPaid = useReInscription ? reInscriptionPaid : inscriptionPaid;
        double insReinsExpected = useReInscription
            ? nvl(feeStructure.getReRegistrationFee())
            : nvl(feeStructure.getRegistrationFee());
        double insReinsRemaining = Math.max(0d, insReinsExpected - insReinsPaid);

        double monthlyFee = nvl(feeStructure.getMonthlyTuitionFee());
        double tuitionPaid = sums.get(Payment.PaymentType.SCOLARITE);
        List<StudentPaymentInfoDTO.MonthlyTuitionStatusDTO> monthlyStatuses = buildMonthlyTuitionStatuses(monthlyFee, tuitionPaid);

        boolean suppliesColumnEnabled = Boolean.TRUE.equals(feeStructure.getSuppliesColumnEnabled());
        double suppliesExpected = suppliesColumnEnabled ? nvl(feeStructure.getSuppliesFee()) : 0d;

        return new StudentPaymentInfoDTO(
            student.getId(),
            schoolClass.getId(),
            (student.getLastName() + " " + student.getFirstName()).trim(),
            student.getMatricule(),
            insReinsType,
            insReinsExpected,
            insReinsPaid,
            insReinsRemaining,
            Boolean.TRUE.equals(account.getSuppliesPaid()),
            suppliesExpected,
            suppliesColumnEnabled,
            monthlyStatuses
        );
    }

    @Transactional
    public CreatePaymentResponse createStudentPayment(Long studentId, CreatePaymentRequest request) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId obligatoire.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Requête de paiement obligatoire.");
        }

        StudentPaymentInfoDTO info = getStudentPaymentInfo(studentId);
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Élève introuvable."));
        Long yearId = student.getSchoolClass().getYear().getId();
        StudentAccount account = studentAccountRepository.findByStudent_IdAndSchoolYear_Id(studentId, yearId)
            .orElseThrow(() -> new IllegalArgumentException("Compte élève introuvable."));

        Payment.PaymentMode paymentMode = parsePaymentMode(request.paymentMode());
        String currency = normalizeCurrency(request.currency());
        double totalCollected;

        double openBalance = computeOpenBalanceTotal(info);
        if (openBalance <= 1e-6) {
            throw new IllegalArgumentException("Aucun reliquat à encaisser pour cet élève.");
        }

        if (request.totalDeclaredAmount() != null && request.totalDeclaredAmount() > 0d) {
            double declared = nvl(request.totalDeclaredAmount());
            if (declared > openBalance + 1e-6) {
                throw new IllegalArgumentException("Le montant dépasse le reliquat dû pour cet élève.");
            }
            totalCollected = allocateAndPersistFromDeclaredTotal(account, info, paymentMode, currency, declared);
        } else {
            double plannedLegacy = computeLegacyPlannedAmount(info, request);
            if (plannedLegacy <= 1e-6) {
                throw new IllegalArgumentException("Aucun paiement à enregistrer.");
            }
            if (plannedLegacy > openBalance + 1e-6) {
                throw new IllegalArgumentException("Le montant dépasse le reliquat dû pour cet élève.");
            }

            totalCollected = 0d;
            if (Boolean.TRUE.equals(request.payInsReins())) {
                double insRem = Math.max(0d, nvl(info.insReinsRemaining()));
                double amount = Math.min(clampAmount(request.insReinsAmount()), insRem);
                if (amount > 0) {
                    Payment p = new Payment();
                    p.setTenantId(account.getTenantId());
                    p.setStudentAccount(account);
                    p.setPaymentMode(paymentMode);
                    p.setCurrency(currency);
                    p.setAmount(amount);
                    p.setPaymentType("REINSCRIPTION".equalsIgnoreCase(info.insReinsType())
                        ? Payment.PaymentType.REINSCRIPTION
                        : Payment.PaymentType.INSCRIPTION);
                    paymentRepository.save(p);
                    totalCollected += amount;
                }
            }

            if (Boolean.TRUE.equals(request.paySupplies()) && info.suppliesColumnEnabled()) {
                account.setSuppliesPaid(true);
                studentAccountRepository.save(account);
            }

            List<StudentPaymentInfoDTO.MonthlyTuitionStatusDTO> openMonths = info.monthlyTuition().stream()
                .filter(m -> !"COMPLET".equalsIgnoreCase(m.status()))
                .toList();
            Map<String, StudentPaymentInfoDTO.MonthlyTuitionStatusDTO> openByCode = openMonths.stream()
                .collect(Collectors.toMap(StudentPaymentInfoDTO.MonthlyTuitionStatusDTO::monthCode, Function.identity()));
            if (request.months() != null) {
                Set<String> seenMonths = new LinkedHashSet<>();
                for (String month : request.months()) {
                    if (!seenMonths.add(month)) {
                        continue;
                    }
                    var m = openByCode.get(month);
                    if (m == null) {
                        continue;
                    }
                    double remain = Math.max(0d, nvl(m.dueAmount()) - nvl(m.paidAmount()));
                    if (remain <= 0) {
                        continue;
                    }
                    Payment p = new Payment();
                    p.setTenantId(account.getTenantId());
                    p.setStudentAccount(account);
                    p.setPaymentMode(paymentMode);
                    p.setCurrency(currency);
                    p.setAmount(remain);
                    p.setPaymentType(Payment.PaymentType.SCOLARITE);
                    paymentRepository.save(p);
                    totalCollected += remain;
                }
            }
        }

        return new CreatePaymentResponse(
            studentId,
            info.schoolClassId(),
            totalCollected,
            paymentMode.name(),
            "RCPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }

    /** Reliquat théorique aligné sur l’écran d’encaissement (inscription + fournitures si impayées + mensualités ouvertes). */
    private double computeOpenBalanceTotal(StudentPaymentInfoDTO info) {
        double open = Math.max(0d, nvl(info.insReinsRemaining()));
        if (info.suppliesColumnEnabled() && !info.suppliesPaid()) {
            open += Math.max(0d, nvl(info.suppliesExpected()));
        }
        for (StudentPaymentInfoDTO.MonthlyTuitionStatusDTO m : info.monthlyTuition()) {
            if ("COMPLET".equalsIgnoreCase(m.status())) {
                continue;
            }
            open += Math.max(0d, nvl(m.dueAmount()) - nvl(m.paidAmount()));
        }
        return open;
    }

    /** Montant « comptable » que la requête legacy vise à solder (hors doublons de mois). */
    private double computeLegacyPlannedAmount(StudentPaymentInfoDTO info, CreatePaymentRequest request) {
        double planned = 0d;
        if (Boolean.TRUE.equals(request.payInsReins())) {
            double insRem = Math.max(0d, nvl(info.insReinsRemaining()));
            planned += Math.min(clampAmount(request.insReinsAmount()), insRem);
        }
        if (Boolean.TRUE.equals(request.paySupplies()) && info.suppliesColumnEnabled() && !info.suppliesPaid()) {
            planned += Math.max(0d, nvl(info.suppliesExpected()));
        }
        if (request.months() != null) {
            List<StudentPaymentInfoDTO.MonthlyTuitionStatusDTO> openMonths = info.monthlyTuition().stream()
                .filter(m -> !"COMPLET".equalsIgnoreCase(m.status()))
                .toList();
            Map<String, StudentPaymentInfoDTO.MonthlyTuitionStatusDTO> openByCode = openMonths.stream()
                .collect(Collectors.toMap(StudentPaymentInfoDTO.MonthlyTuitionStatusDTO::monthCode, Function.identity()));
            Set<String> seen = new LinkedHashSet<>();
            for (String month : request.months()) {
                if (!seen.add(month)) {
                    continue;
                }
                var m = openByCode.get(month);
                if (m == null) {
                    continue;
                }
                planned += Math.max(0d, nvl(m.dueAmount()) - nvl(m.paidAmount()));
            }
        }
        return planned;
    }

    /**
     * Répartit {@code total} dans l'ordre : inscription/réinscription (partiel possible), fournitures (uniquement si solde complet),
     * puis mensualités Oct→Juin (partiel possible). Le total encaissé doit éponger exactement le reliquat ouvert.
     */
    private double allocateAndPersistFromDeclaredTotal(
        StudentAccount account,
        StudentPaymentInfoDTO info,
        Payment.PaymentMode paymentMode,
        String currency,
        double total
    ) {
        if (total <= 0d) {
            throw new IllegalArgumentException("Montant invalide.");
        }
        double R = total;
        double collected = 0d;

        double insRem = Math.max(0d, nvl(info.insReinsRemaining()));
        if (insRem > 0d && R > 0d) {
            double pay = Math.min(R, insRem);
            Payment p = new Payment();
            p.setTenantId(account.getTenantId());
            p.setStudentAccount(account);
            p.setPaymentMode(paymentMode);
            p.setCurrency(currency);
            p.setAmount(pay);
            p.setPaymentType("REINSCRIPTION".equalsIgnoreCase(info.insReinsType())
                ? Payment.PaymentType.REINSCRIPTION
                : Payment.PaymentType.INSCRIPTION);
            paymentRepository.save(p);
            collected += pay;
            R -= pay;
        }

        if (info.suppliesColumnEnabled() && !info.suppliesPaid()) {
            double sup = Math.max(0d, nvl(info.suppliesExpected()));
            if (sup > 0d && R >= sup) {
                account.setSuppliesPaid(true);
                studentAccountRepository.save(account);
                collected += sup;
                R -= sup;
            }
        }

        for (StudentPaymentInfoDTO.MonthlyTuitionStatusDTO m : info.monthlyTuition()) {
            if (R <= 0d) {
                break;
            }
            if ("COMPLET".equalsIgnoreCase(m.status())) {
                continue;
            }
            double remain = Math.max(0d, nvl(m.dueAmount()) - nvl(m.paidAmount()));
            if (remain <= 0d) {
                continue;
            }
            double pay = Math.min(R, remain);
            if (pay > 0d) {
                Payment p = new Payment();
                p.setTenantId(account.getTenantId());
                p.setStudentAccount(account);
                p.setPaymentMode(paymentMode);
                p.setCurrency(currency);
                p.setAmount(pay);
                p.setPaymentType(Payment.PaymentType.SCOLARITE);
                paymentRepository.save(p);
                collected += pay;
                R -= pay;
            }
        }

        if (R > 0.01d) {
            throw new IllegalArgumentException("Le montant dépasse le reliquat ouvert pour cet élève.");
        }
        return collected;
    }

    private StudentPaymentStatusDTO toStatus(
        Student student,
        Map<Long, StudentAccount> accountByStudentId,
        Map<Long, PaymentSums> paidByAccountId,
        FeeStructure feeStructure,
        double monthlyFee,
        double suppliesFee,
        double tuitionExpected,
        double totalExpected
    ) {
        StudentAccount account = accountByStudentId.get(student.getId());
        PaymentSums sums = account == null ? PaymentSums.empty() : paidByAccountId.getOrDefault(account.getId(), PaymentSums.empty());

        double inscriptionPaid = sums.get(Payment.PaymentType.INSCRIPTION);
        double reInscriptionPaid = sums.get(Payment.PaymentType.REINSCRIPTION);
        boolean useReInscription = reInscriptionPaid > 0d;
        String insReinsLabel = useReInscription ? "REINS" : "INS";
        double insReinsPaid = useReInscription ? reInscriptionPaid : inscriptionPaid;
        double insReinsExpected = useReInscription
            ? nvl(feeStructure.getReRegistrationFee())
            : nvl(feeStructure.getRegistrationFee());

        double totalPaid = sums.total();
        double tuitionPaid = sums.get(Payment.PaymentType.SCOLARITE);
        double paymentPercentage = tuitionExpected <= 0d ? 100d : (tuitionPaid / tuitionExpected) * 100d;
        boolean suppliesColumnEnabled = Boolean.TRUE.equals(feeStructure.getSuppliesColumnEnabled());
        boolean hasPaidSupplies = Boolean.TRUE.equals(account != null ? account.getSuppliesPaid() : Boolean.FALSE);

        Map<String, Boolean> monthlyCoverage = buildMonthlyCoverage(tuitionPaid, monthlyFee);

        return new StudentPaymentStatusDTO(
            student.getId(),
            student.getLastName(),
            student.getFirstName(),
            student.getMatricule(),
            resolvePhone(student),
            insReinsLabel,
            insReinsPaid,
            insReinsExpected,
            suppliesColumnEnabled,
            hasPaidSupplies,
            suppliesFee,
            tuitionPaid,
            tuitionExpected,
            totalPaid,
            totalExpected,
            Math.max(0d, Math.min(100d, paymentPercentage)),
            monthlyCoverage
        );
    }

    private Map<Long, PaymentSums> groupPaymentSumsByAccount(List<Payment> payments) {
        Map<Long, PaymentSums> byAccount = new LinkedHashMap<>();
        for (Payment p : payments) {
            Long accountId = p.getStudentAccount().getId();
            byAccount.computeIfAbsent(accountId, ignored -> PaymentSums.empty())
                .add(p.getPaymentType(), nvl(p.getAmount()));
        }
        return byAccount;
    }

    private Map<String, Boolean> buildMonthlyCoverage(double tuitionPaid, double monthlyFee) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        if (monthlyFee <= 0d) {
            MONTHS_OCT_TO_JUN.forEach(m -> result.put(m, true));
            return result;
        }
        int coveredCount = (int) Math.floor(tuitionPaid / monthlyFee);
        for (int i = 0; i < MONTHS_OCT_TO_JUN.size(); i++) {
            result.put(MONTHS_OCT_TO_JUN.get(i), i < coveredCount);
        }
        return result;
    }

    private List<StudentPaymentInfoDTO.MonthlyTuitionStatusDTO> buildMonthlyTuitionStatuses(double monthlyFee, double tuitionPaid) {
        double remaining = Math.max(0d, tuitionPaid);
        var rows = new java.util.ArrayList<StudentPaymentInfoDTO.MonthlyTuitionStatusDTO>(MONTHS_OCT_TO_JUN.size());
        for (int i = 0; i < MONTHS_OCT_TO_JUN.size(); i++) {
            double due = Math.max(0d, monthlyFee);
            double paid = Math.min(due, remaining);
            remaining = Math.max(0d, remaining - paid);
            String status = paid <= 0d ? "NON_PAYE" : (paid >= due ? "COMPLET" : "PARTIEL");
            rows.add(new StudentPaymentInfoDTO.MonthlyTuitionStatusDTO(
                MONTHS_OCT_TO_JUN.get(i),
                MONTHS_LABELS_OCT_TO_JUN.get(i),
                due,
                paid,
                status
            ));
        }
        return rows;
    }

    private String resolvePhone(Student student) {
        if (student.getEmergencyContactPhone() != null && !student.getEmergencyContactPhone().isBlank()) {
            return student.getEmergencyContactPhone();
        }
        if (student.getFather() != null && student.getFather().getPhone() != null && !student.getFather().getPhone().isBlank()) {
            return student.getFather().getPhone();
        }
        if (student.getMother() != null && student.getMother().getPhone() != null && !student.getMother().getPhone().isBlank()) {
            return student.getMother().getPhone();
        }
        return "";
    }

    private double nvl(Double value) {
        return value == null ? 0d : value;
    }

    private double clampAmount(Double value) {
        return Math.max(0d, nvl(value));
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "GNF";
        }
        return currency.trim().toUpperCase();
    }

    private Payment.PaymentMode parsePaymentMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Payment.PaymentMode.ESPECES;
        }
        try {
            return Payment.PaymentMode.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Mode de paiement invalide.");
        }
    }

    private static class PaymentSums {
        private final Map<Payment.PaymentType, Double> byType = new EnumMap<>(Payment.PaymentType.class);

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

        double total() {
            return byType.values().stream().mapToDouble(Double::doubleValue).sum();
        }
    }
}


package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.StudentPaymentLedgerRowDTO;
import friasoft.gn.schoolapp.dto.StudentPaymentStatusDTO;
import friasoft.gn.schoolapp.dto.StudentPaymentInfoDTO;
import friasoft.gn.schoolapp.dto.FinancePaymentDtos.CreatePaymentRequest;
import friasoft.gn.schoolapp.dto.FinancePaymentDtos.CreatePaymentResponse;
import friasoft.gn.schoolapp.dto.FinancePaymentDtos.PaymentReceiptView;
import friasoft.gn.schoolapp.dto.FinancePaymentDtos.ReceiptLine;
import friasoft.gn.schoolapp.entity.auth.User;
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
import friasoft.gn.schoolapp.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.EnumMap;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    private static final List<String> MONTHS_OCT_TO_JUN = List.of("OCT", "NOV", "DEC", "JAN", "FEB", "MAR", "APR", "MAY", "JUN");
    private static final List<String> MONTHS_LABELS_OCT_TO_JUN = List.of("Octobre", "Novembre", "Décembre", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin");

    private final ISchoolClassRepository schoolClassRepository;
    private final IStudentRepository studentRepository;
    private final IStudentAccountRepository studentAccountRepository;
    private final IPaymentRepository paymentRepository;
    private final IFeeStructureRepository feeStructureRepository;
    private final SchoolService schoolService;
    private final UserRepository userRepository;
    private final PaymentConfirmationMailService paymentConfirmationMailService;

    public FinanceService(
        ISchoolClassRepository schoolClassRepository,
        IStudentRepository studentRepository,
        IStudentAccountRepository studentAccountRepository,
        IPaymentRepository paymentRepository,
        IFeeStructureRepository feeStructureRepository,
        SchoolService schoolService,
        UserRepository userRepository,
        @Lazy PaymentConfirmationMailService paymentConfirmationMailService
    ) {
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
        this.studentAccountRepository = studentAccountRepository;
        this.paymentRepository = paymentRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.schoolService = schoolService;
        this.userRepository = userRepository;
        this.paymentConfirmationMailService = paymentConfirmationMailService;
    }

    @Transactional(readOnly = true)
    public List<StudentPaymentLedgerRowDTO> listPaymentLedgerForStudent(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId obligatoire.");
        }
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Élève introuvable."));
        if (student.getSchoolClass() == null || student.getSchoolClass().getYear() == null
            || student.getSchoolClass().getYear().getSchool() == null) {
            throw new IllegalArgumentException("Contexte classe introuvable.");
        }
        schoolService.assertCurrentUserCanAccessSchool(student.getSchoolClass().getYear().getSchool().getId());
        return paymentRepository.findAllByStudentIdOrderByPaymentDateDesc(studentId).stream()
            .map(this::toLedgerRow)
            .toList();
    }

    private StudentPaymentLedgerRowDTO toLedgerRow(Payment p) {
        StudentAccount a = p.getStudentAccount();
        String yearLabel = (a.getSchoolYear() != null && a.getSchoolYear().getLabel() != null)
            ? a.getSchoolYear().getLabel()
            : "—";
        return new StudentPaymentLedgerRowDTO(
            p.getId(),
            p.getPaymentType() != null ? p.getPaymentType().name() : null,
            p.getAmount(),
            p.getCurrency(),
            p.getPaymentMode() != null ? p.getPaymentMode().name() : null,
            p.getPaymentDate(),
            yearLabel,
            p.getReceiptReference(),
            p.getRecordedBy(),
            p.getValidatedBy() != null ? p.getValidatedBy().getFullname() : null,
            tuitionMonthLabelForPayment(p)
        );
    }

    private void attachPaymentValidator(Payment p) {
        User u = currentUserOrNull();
        if (u == null || u.getId() == null) {
            return;
        }
        p.setValidatedBy(userRepository.getReferenceById(u.getId()));
    }

    private static User currentUserOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }

    private static String tuitionMonthLabelForPayment(Payment p) {
        if (p.getPaymentType() != Payment.PaymentType.SCOLARITE) {
            return null;
        }
        return monthLabelFromMonthCode(p.getTuitionMonthCode());
    }

    private static String monthLabelFromMonthCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String c = code.trim().toUpperCase(Locale.ROOT);
        int idx = MONTHS_OCT_TO_JUN.indexOf(c);
        return idx >= 0 ? MONTHS_LABELS_OCT_TO_JUN.get(idx) : code.trim();
    }

    private void saveFournituresPayment(
        StudentAccount account,
        Payment.PaymentMode paymentMode,
        String currency,
        double amount,
        String receiptReference,
        String recordedBy,
        List<ReceiptLine> linesOut
    ) {
        if (amount <= 0d) {
            return;
        }
        Payment p = new Payment();
        p.setTenantId(account.getTenantId());
        p.setStudentAccount(account);
        p.setPaymentMode(paymentMode);
        p.setCurrency(currency);
        p.setAmount(amount);
        p.setPaymentType(Payment.PaymentType.FOURNITURES);
        p.setReceiptReference(receiptReference);
        p.setRecordedBy(recordedBy);
        attachPaymentValidator(p);
        paymentRepository.save(p);
        appendReceiptLine(linesOut, Payment.PaymentType.FOURNITURES, amount, null);
    }

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

        List<Payment> accountPayments = paymentRepository.findByStudentAccount_IdIn(List.of(account.getId()));
        PaymentSums sums = groupPaymentSumsByAccount(accountPayments)
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
        Map<String, Double> explicitTuitionByMonth = new HashMap<>();
        double orphanTuition = 0d;
        for (Payment p : accountPayments) {
            if (p.getPaymentType() != Payment.PaymentType.SCOLARITE) {
                continue;
            }
            double amt = nvl(p.getAmount());
            String rawCode = p.getTuitionMonthCode();
            if (rawCode == null || rawCode.isBlank()) {
                orphanTuition += amt;
            } else {
                explicitTuitionByMonth.merge(rawCode.trim().toUpperCase(Locale.ROOT), amt, Double::sum);
            }
        }
        List<StudentPaymentInfoDTO.MonthlyTuitionStatusDTO> monthlyStatuses =
            buildMonthlyTuitionStatuses(monthlyFee, explicitTuitionByMonth, orphanTuition);

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

    /**
     * Répartit un montant saisi sur inscription/réinscription, fournitures (si intégral), puis mensualités — même ordre que l’encaissement.
     * Utilisé après création du compte élève (inscription).
     */
    @Transactional
    public double allocateDeclaredTotalForNewStudentAccount(Long studentId, double total, String paymentModeRaw, String currency) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId obligatoire.");
        }
        if (total <= 0d) {
            return 0d;
        }
        StudentPaymentInfoDTO info = getStudentPaymentInfo(studentId);
        double open = computeOpenBalanceTotal(info);
        if (total > open + 1e-6) {
            throw new IllegalArgumentException("Le montant dépasse le reliquat dû pour cet élève.");
        }
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Élève introuvable."));
        Long yearId = student.getSchoolClass().getYear().getId();
        StudentAccount account = studentAccountRepository.findByStudent_IdAndSchoolYear_Id(studentId, yearId)
            .orElseThrow(() -> new IllegalArgumentException("Compte élève introuvable."));
        Payment.PaymentMode paymentMode = parsePaymentMode(paymentModeRaw);
        String ref = newReceiptReference();
        return allocateAndPersistFromDeclaredTotal(
            account, info, paymentMode, normalizeCurrency(currency), total, ref, "Inscription", null);
    }

    @Transactional
    public CreatePaymentResponse createStudentPayment(Long studentId, CreatePaymentRequest request) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId obligatoire.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Requête de paiement obligatoire.");
        }
        String recordedBy = requireRecordedBy(request.recordedBy());
        String receiptRef = newReceiptReference();
        List<ReceiptLine> receiptLines = new ArrayList<>();

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
            totalCollected = allocateAndPersistFromDeclaredTotal(
                account, info, paymentMode, currency, declared, receiptRef, recordedBy, receiptLines);
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
                    p.setReceiptReference(receiptRef);
                    p.setRecordedBy(recordedBy);
                    p.setPaymentType("REINSCRIPTION".equalsIgnoreCase(info.insReinsType())
                        ? Payment.PaymentType.REINSCRIPTION
                        : Payment.PaymentType.INSCRIPTION);
                    attachPaymentValidator(p);
                    paymentRepository.save(p);
                    appendReceiptLine(receiptLines, p.getPaymentType(), amount, null);
                    totalCollected += amount;
                }
            }

            if (Boolean.TRUE.equals(request.paySupplies()) && info.suppliesColumnEnabled() && !info.suppliesPaid()) {
                double supRem = Math.max(0d, nvl(info.suppliesExpected()));
                if (supRem > 0d) {
                    account.setSuppliesPaid(true);
                    studentAccountRepository.save(account);
                    saveFournituresPayment(account, paymentMode, currency, supRem, receiptRef, recordedBy, receiptLines);
                    totalCollected += supRem;
                }
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
                    p.setReceiptReference(receiptRef);
                    p.setRecordedBy(recordedBy);
                    p.setPaymentType(Payment.PaymentType.SCOLARITE);
                    String mCode = m.monthCode() != null ? m.monthCode().trim().toUpperCase(Locale.ROOT) : null;
                    p.setTuitionMonthCode(mCode);
                    attachPaymentValidator(p);
                    paymentRepository.save(p);
                    appendReceiptLine(receiptLines, Payment.PaymentType.SCOLARITE, remain, m.monthLabel());
                    totalCollected += remain;
                }
            }
        }

        var response = new CreatePaymentResponse(
            studentId,
            info.schoolClassId(),
            totalCollected,
            paymentMode.name(),
            receiptRef,
            recordedBy,
            List.copyOf(receiptLines)
        );
        final double totalMail = totalCollected;
        final String receiptForMail = receiptRef;
        studentRepository.findByIdWithParentsAndClass(studentId)
            .ifPresent(s -> paymentConfirmationMailService.sendPaymentConfirmation(s, receiptForMail, totalMail));
        return response;
    }

    @Transactional(readOnly = true)
    public PaymentReceiptView getReceiptDuplicate(Long studentId, String reference) {
        if (studentId == null || reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("studentId et reference obligatoires.");
        }
        String ref = reference.trim();
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Élève introuvable."));
        if (student.getSchoolClass() == null || student.getSchoolClass().getYear() == null
            || student.getSchoolClass().getYear().getSchool() == null) {
            throw new IllegalArgumentException("Contexte classe introuvable.");
        }
        schoolService.assertCurrentUserCanAccessSchool(student.getSchoolClass().getYear().getSchool().getId());
        List<Payment> list = paymentRepository.findByStudentIdAndReceiptReference(studentId, ref);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Aucun paiement pour cette référence.");
        }
        Payment first = list.get(0);
        StudentAccount acc = first.getStudentAccount();
        String yearLabel = acc.getSchoolYear() != null && acc.getSchoolYear().getLabel() != null
            ? acc.getSchoolYear().getLabel()
            : "—";
        List<ReceiptLine> lines = list.stream()
            .map(p -> new ReceiptLine(
                p.getPaymentType() != null ? p.getPaymentType().name() : "",
                p.getAmount(),
                p.getPaymentType() == Payment.PaymentType.SCOLARITE ? monthLabelFromMonthCode(p.getTuitionMonthCode()) : null
            ))
            .toList();
        double total = list.stream().mapToDouble(p -> nvl(p.getAmount())).sum();
        LocalDateTime when = list.stream()
            .map(Payment::getPaymentDate)
            .max(LocalDateTime::compareTo)
            .orElse(first.getPaymentDate());
        String mode = first.getPaymentMode() != null ? first.getPaymentMode().name() : "ESPECES";
        String cur = first.getCurrency() != null ? first.getCurrency() : "GNF";
        return new PaymentReceiptView(
            (student.getLastName() + " " + student.getFirstName()).trim(),
            student.getMatricule(),
            yearLabel,
            ref,
            first.getRecordedBy(),
            mode,
            cur,
            when,
            lines,
            total,
            true
        );
    }

    private static String newReceiptReference() {
        return "RCPT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private static String requireRecordedBy(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("L'auteur du paiement est obligatoire.");
        }
        return raw.trim();
    }

    private static void appendReceiptLine(
        List<ReceiptLine> out,
        Payment.PaymentType type,
        double amount,
        String tuitionMonthLabel
    ) {
        if (out == null || amount <= 0d || type == null) {
            return;
        }
        out.add(new ReceiptLine(type.name(), amount, tuitionMonthLabel));
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
        double total,
        String receiptReference,
        String recordedBy,
        List<ReceiptLine> linesOut
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
            p.setReceiptReference(receiptReference);
            p.setRecordedBy(recordedBy);
            p.setPaymentType("REINSCRIPTION".equalsIgnoreCase(info.insReinsType())
                ? Payment.PaymentType.REINSCRIPTION
                : Payment.PaymentType.INSCRIPTION);
            attachPaymentValidator(p);
            paymentRepository.save(p);
            appendReceiptLine(linesOut, p.getPaymentType(), pay, null);
            collected += pay;
            R -= pay;
        }

        if (info.suppliesColumnEnabled() && !info.suppliesPaid()) {
            double sup = Math.max(0d, nvl(info.suppliesExpected()));
            if (sup > 0d && R >= sup) {
                account.setSuppliesPaid(true);
                studentAccountRepository.save(account);
                saveFournituresPayment(account, paymentMode, currency, sup, receiptReference, recordedBy, linesOut);
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
                p.setReceiptReference(receiptReference);
                p.setRecordedBy(recordedBy);
                p.setPaymentType(Payment.PaymentType.SCOLARITE);
                String mCode = m.monthCode() != null ? m.monthCode().trim().toUpperCase(Locale.ROOT) : null;
                p.setTuitionMonthCode(mCode);
                attachPaymentValidator(p);
                paymentRepository.save(p);
                appendReceiptLine(linesOut, Payment.PaymentType.SCOLARITE, pay, m.monthLabel());
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
        boolean hasPaidSupplies = Boolean.TRUE.equals(account != null ? account.getSuppliesPaid() : Boolean.FALSE)
            || sums.get(Payment.PaymentType.FOURNITURES) > 0d;

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

    /**
     * Soldes par mois : montants explicitement rattachés à un mois (nouveaux encaissements) + reliquat « sans mois »
     * réparti en Oct→Juin (comportement historique pour les anciennes lignes SCOLARITE).
     */
    private List<StudentPaymentInfoDTO.MonthlyTuitionStatusDTO> buildMonthlyTuitionStatuses(
        double monthlyFee,
        Map<String, Double> explicitByMonth,
        double orphanTuition
    ) {
        var rows = new ArrayList<StudentPaymentInfoDTO.MonthlyTuitionStatusDTO>(MONTHS_OCT_TO_JUN.size());
        double orphanRem = Math.max(0d, orphanTuition);
        for (int i = 0; i < MONTHS_OCT_TO_JUN.size(); i++) {
            String code = MONTHS_OCT_TO_JUN.get(i);
            double due = Math.max(0d, monthlyFee);
            double explicit = nvl(explicitByMonth.get(code));
            double needFromOrphan = Math.max(0d, due - explicit);
            double fromOrphan = Math.min(orphanRem, needFromOrphan);
            double paid = explicit + fromOrphan;
            orphanRem -= fromOrphan;
            String status = paid <= 0d ? "NON_PAYE" : (paid + 1e-6 >= due ? "COMPLET" : "PARTIEL");
            rows.add(new StudentPaymentInfoDTO.MonthlyTuitionStatusDTO(
                code,
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


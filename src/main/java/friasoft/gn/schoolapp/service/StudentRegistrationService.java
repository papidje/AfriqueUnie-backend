package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.FamilyPreviewDtos.FamilyPreviewResponse;
import friasoft.gn.schoolapp.dto.FamilyPreviewDtos.ParentSummary;
import friasoft.gn.schoolapp.dto.FamilyPreviewDtos.SiblingStudentRow;
import friasoft.gn.schoolapp.dto.LegalGuardianRegistrationDTO;
import friasoft.gn.schoolapp.dto.RegistrationDTO;
import friasoft.gn.schoolapp.dto.StudentRegistrationDTO;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.Parent;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.entity.school.StudentAccount;
import friasoft.gn.schoolapp.repository.IStudentAccountRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.service.finance.TuitionMonthDues;
import friasoft.gn.schoolapp.util.GuineaContactValidation;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class StudentRegistrationService {

    private final ParentService parentService;
    private final StudentService studentService;
    private final ISchoolClassRepository schoolClassRepository;
    private final SchoolService schoolService;
    private final IStudentAccountRepository studentAccountRepository;
    private final IStudentRepository studentRepository;
    private final FinanceService financeService;

    @Transactional
    public Student registerStudent(RegistrationDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("RegistrationDTO obligatoire.");
        }
        if (dto.student() == null) {
            throw new IllegalArgumentException("Infos élève obligatoires.");
        }
        if (dto.classId() == null) {
            throw new IllegalArgumentException("classId obligatoire.");
        }
        if (dto.legalGuardian() == null) {
            throw new IllegalArgumentException("Représentant légal obligatoire.");
        }
        double amountPaid = dto.amountPaid() == null ? 0d : dto.amountPaid();
        if (amountPaid < 0) {
            throw new IllegalArgumentException("amountPaid doit être >= 0.");
        }

        Long tenantId = requireTenantIdFromSecurity();

        StudentRegistrationDTO studentDto = dto.student();
        var civility = parseCivility(studentDto.civility());

        SchoolClassInfo loaded = loadSchoolClassAndAssertAccess(dto.classId());

        Student student = new Student();
        student.setCivility(civility);
        student.setFirstName(nonBlank(studentDto.firstName(), "firstName obligatoire."));
        student.setLastName(nonBlank(studentDto.lastName(), "lastName obligatoire."));
        if (studentDto.birthDate() == null) {
            throw new IllegalArgumentException("birthDate obligatoire.");
        }
        student.setBirthDate(studentDto.birthDate());
        student.setBirthPlace(trimToNull(studentDto.birthPlace()));
        student.setNationality(trimToNull(studentDto.nationality()));
        student.setAddress(trimToNull(studentDto.address()));
        student.setCommunicationPhone(normalizePhoneOrNull(studentDto.communicationPhone()));
        student.setCommunicationEmail(trimToNull(studentDto.communicationEmail()));
        validateOptionalEmail(studentDto.communicationEmail(), "Email de communication");
        applyLegalGuardian(student, dto.legalGuardian());
        student.setEmergencyContactName(trimToNull(studentDto.emergencyContactName()));
        student.setEmergencyContactPhone(normalizePhoneOrNull(studentDto.emergencyContactPhone()));
        student.setBloodGroup(trimToNull(studentDto.bloodGroup()));
        student.setAllergies(trimToNull(studentDto.allergies()));
        student.setCardNumber(trimToNull(studentDto.cardNumber()));
        student.setSchoolClass(loaded.schoolClass());
        student.setSchool(loaded.schoolClass().getYear().getSchool());
        student.setTenantId(tenantId);
        student.setEnrollmentStatus(Student.EnrollmentStatus.INSCRIT);

        Student savedStudent = studentService.save(student);

        StudentAccount account = new StudentAccount();
        account.setTenantId(tenantId);
        account.setStudent(savedStudent);
        account.setSchoolYear(loaded.schoolClass().getYear());
        account.setCurrency(normalizeCurrency(dto.currency()));
        account.setSuppliesPaid(false);
        account.setTuitionPayablePercent(normalizeTuitionPayablePercent(dto.tuitionPayablePercent()));
        account = studentAccountRepository.save(account);

        if (amountPaid > 0d) {
            financeService.allocateDeclaredTotalForNewStudentAccount(
                savedStudent.getId(),
                amountPaid,
                dto.paymentMode(),
                normalizeCurrency(dto.currency())
            );
        }

        return savedStudent;
    }

    /**
     * Aperçu fratrie pour l’étape scolarité.
     * Au moins un téléphone (père et/ou mère) ; les deux optionnels individuellement.
     */
    @Transactional(readOnly = true)
    public FamilyPreviewResponse previewFamily(String fatherPhoneRaw, String motherPhoneRaw) {
        String fatherPhone = trimToNull(fatherPhoneRaw);
        String motherPhone = trimToNull(motherPhoneRaw);
        if (fatherPhone == null && motherPhone == null) {
            return new FamilyPreviewResponse(
                emptyParentSummary(null),
                emptyParentSummary(null),
                List.of(),
                List.of(),
                List.of()
            );
        }

        Optional<Parent> fatherOpt = Optional.empty();
        Optional<Parent> motherOpt = Optional.empty();
        if (fatherPhone != null) {
            fatherPhone = normalizePhone(fatherPhone);
            GuineaContactValidation.requireValidGuineaPhone(fatherPhone, "Téléphone du père");
            fatherOpt = parentService.findByPhone(fatherPhone);
        }
        if (motherPhone != null) {
            motherPhone = normalizePhone(motherPhone);
            GuineaContactValidation.requireValidGuineaPhone(motherPhone, "Téléphone de la mère");
            motherOpt = parentService.findByPhone(motherPhone);
        }

        ParentSummary fatherSummary = toParentSummary(fatherOpt, fatherPhone);
        ParentSummary motherSummary = toParentSummary(motherOpt, motherPhone);

        if (fatherOpt.isEmpty() && motherOpt.isEmpty()) {
            return new FamilyPreviewResponse(fatherSummary, motherSummary, List.of(), List.of(), List.of());
        }

        List<Student> both = List.of();
        List<Student> fatherChildren = List.of();
        List<Student> motherChildren = List.of();

        if (fatherOpt.isPresent() && motherOpt.isPresent()) {
            both = studentRepository.findAllByFatherIdAndMotherIdWithClass(
                fatherOpt.get().getId(), motherOpt.get().getId()
            );
        }
        if (fatherOpt.isPresent()) {
            fatherChildren = studentRepository.findAllByFatherIdWithClass(fatherOpt.get().getId());
        }
        if (motherOpt.isPresent()) {
            motherChildren = studentRepository.findAllByMotherIdWithClass(motherOpt.get().getId());
        }

        List<SiblingStudentRow> bothRows = both.stream().map(this::toSiblingRow).toList();
        java.util.Set<Long> bothIds = both.stream().map(Student::getId).collect(java.util.stream.Collectors.toSet());

        List<SiblingStudentRow> fatherOnly = new ArrayList<>();
        for (Student s : fatherChildren) {
            if (!bothIds.contains(s.getId())) {
                fatherOnly.add(toSiblingRow(s));
            }
        }
        List<SiblingStudentRow> motherOnly = new ArrayList<>();
        for (Student s : motherChildren) {
            if (!bothIds.contains(s.getId())) {
                motherOnly.add(toSiblingRow(s));
            }
        }

        return new FamilyPreviewResponse(fatherSummary, motherSummary, bothRows, fatherOnly, motherOnly);
    }

    private void applyLegalGuardian(Student student, LegalGuardianRegistrationDTO guardian) {
        String relation = nonBlank(guardian.relation(), "Lien avec l'élève obligatoire.").trim().toUpperCase();
        String lastName = nonBlank(guardian.lastName(), "Nom du représentant obligatoire.");
        String firstName = nonBlank(guardian.firstName(), "Prénom du représentant obligatoire.");
        validateOptionalEmail(guardian.email(), "Email du représentant");
        String phone = normalizePhoneOrNull(guardian.phone());

        switch (relation) {
            case "PERE" -> {
                Parent father = parentService.resolveOrCreate(
                    new friasoft.gn.schoolapp.dto.ParentDtos.ParentWriteRequest(
                        lastName,
                        firstName,
                        phone,
                        trimToNull(guardian.email()),
                        trimToNull(guardian.profession()),
                        trimToNull(guardian.address())
                    )
                );
                student.setFather(father);
                student.setMother(null);
            }
            case "MERE" -> {
                Parent mother = parentService.resolveOrCreate(
                    new friasoft.gn.schoolapp.dto.ParentDtos.ParentWriteRequest(
                        lastName,
                        firstName,
                        phone,
                        trimToNull(guardian.email()),
                        trimToNull(guardian.profession()),
                        trimToNull(guardian.address())
                    )
                );
                student.setMother(mother);
                student.setFather(null);
            }
            case "TUTEUR" -> {
                Parent tutor = parentService.resolveOrCreate(
                    new friasoft.gn.schoolapp.dto.ParentDtos.ParentWriteRequest(
                        lastName,
                        firstName,
                        phone,
                        trimToNull(guardian.email()),
                        trimToNull(guardian.profession()),
                        trimToNull(guardian.address())
                    )
                );
                student.setTutor(tutor);
            }
            default -> throw new IllegalArgumentException("Lien invalide (attendu PERE|MERE|TUTEUR).");
        }
    }

    private ParentSummary toParentSummary(Optional<Parent> opt, String phone) {
        if (opt.isEmpty()) {
            return emptyParentSummary(phone);
        }
        Parent p = opt.get();
        return new ParentSummary(
            p.getId(),
            p.getFirstName(),
            p.getLastName(),
            p.getPhone(),
            p.getEmail(),
            p.getProfession(),
            p.getAddress(),
            true
        );
    }

    private static ParentSummary emptyParentSummary(String phone) {
        return new ParentSummary(null, null, null, phone, null, null, null, false);
    }

    private SiblingStudentRow toSiblingRow(Student s) {
        String className = s.getSchoolClass() != null ? s.getSchoolClass().getName() : null;
        String status = s.getEnrollmentStatus() != null ? s.getEnrollmentStatus().name() : null;
        return new SiblingStudentRow(
            s.getId(),
            s.getFirstName(),
            s.getLastName(),
            s.getMatricule(),
            className,
            status
        );
    }

    private static double normalizeTuitionPayablePercent(Double raw) {
        if (raw == null) {
            return 100d;
        }
        return TuitionMonthDues.clampPercent(raw);
    }

    private record SchoolClassInfo(friasoft.gn.schoolapp.entity.school.SchoolClass schoolClass) {}

    private SchoolClassInfo loadSchoolClassAndAssertAccess(Long classId) {
        var sc = schoolClassRepository.findByIdWithYearAndSchool(classId)
            .orElseThrow(() -> new IllegalArgumentException("SchoolClass introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(sc.getYear().getSchool().getId());
        return new SchoolClassInfo(sc);
    }

    private static Long requireTenantIdFromSecurity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Contexte utilisateur introuvable.");
        }
        Long tenantId = user.getOrganizationTenantId();
        if (tenantId == null) {
            tenantId = user.getTenantId();
        }
        if (tenantId == null) {
            throw new IllegalStateException("tenantId manquant dans le contexte.");
        }
        return tenantId;
    }

    private static Student.Civility parseCivility(String civility) {
        if (civility == null || civility.isBlank()) {
            throw new IllegalArgumentException("civilité obligatoire.");
        }
        try {
            return Student.Civility.valueOf(civility.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("civilité invalide (attendu MONSIEUR|MADAME).", e);
        }
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "GNF";
        }
        return currency.trim().toUpperCase();
    }

    private static String normalizePhone(String phone) {
        return GuineaContactValidation.compactPhone(phone);
    }

    private static String normalizePhoneOrNull(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String compact = GuineaContactValidation.compactPhone(phone);
        GuineaContactValidation.requireValidGuineaPhone(compact, "Téléphone");
        return compact;
    }

    private static void validateOptionalEmail(String email, String fieldLabel) {
        GuineaContactValidation.requireValidEmail(email, fieldLabel);
    }

    private static String nonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String out = value.trim();
        return out.isEmpty() ? null : out;
    }
}

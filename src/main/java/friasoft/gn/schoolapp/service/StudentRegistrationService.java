package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ParentRegistrationDTO;
import friasoft.gn.schoolapp.dto.RegistrationDTO;
import friasoft.gn.schoolapp.dto.StudentRegistrationDTO;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.Parent;
import friasoft.gn.schoolapp.entity.school.Payment;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.entity.school.StudentAccount;
import friasoft.gn.schoolapp.repository.IStudentAccountRepository;
import friasoft.gn.schoolapp.repository.IPaymentRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class StudentRegistrationService {

    private final ParentService parentService;
    private final StudentService studentService;
    private final ISchoolClassRepository schoolClassRepository;
    private final SchoolService schoolService;
    private final IStudentAccountRepository studentAccountRepository;
    private final IPaymentRepository paymentRepository;

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
        if (dto.amountPaid() == null) {
            throw new IllegalArgumentException("amountPaid obligatoire.");
        }
        if (dto.amountPaid() < 0) {
            throw new IllegalArgumentException("amountPaid doit être >= 0.");
        }

        Long tenantId = requireTenantIdFromSecurity();

        StudentRegistrationDTO studentDto = dto.student();
        var civility = parseCivility(studentDto.civility());

        SchoolClassInfo loaded = loadSchoolClassAndAssertAccess(dto.classId());

        Parent father = resolveOrCreateParent(dto.father(), tenantId);
        Parent mother = resolveOrCreateParent(dto.mother(), tenantId);

        Student student = new Student();
        student.setCivility(civility);
        student.setFirstName(nonBlank(studentDto.firstName(), "firstName obligatoire."));
        student.setLastName(nonBlank(studentDto.lastName(), "lastName obligatoire."));
        if (studentDto.birthDate() == null) {
            throw new IllegalArgumentException("birthDate obligatoire.");
        }
        student.setBirthDate(studentDto.birthDate());
        student.setFather(father);
        student.setMother(mother);
        student.setEmergencyContactName(nonBlank(studentDto.emergencyContactName(), "Emergency contact name obligatoire."));
        student.setEmergencyContactPhone(nonBlank(studentDto.emergencyContactPhone(), "Emergency contact phone obligatoire."));
        student.setSchoolClass(loaded.schoolClass());
        student.setTenantId(tenantId);

        Student savedStudent = studentService.save(student);

        StudentAccount account = new StudentAccount();
        account.setTenantId(tenantId);
        account.setStudent(savedStudent);
        account.setSchoolYear(loaded.schoolClass().getYear());
        account.setCurrency(normalizeCurrency(dto.currency()));
        account.setSuppliesPaid(false);
        account = studentAccountRepository.save(account);

        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setStudentAccount(account);
        payment.setPaymentType(Payment.PaymentType.INSCRIPTION);
        payment.setAmount(dto.amountPaid());
        payment.setCurrency(normalizeCurrency(dto.currency()));
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        return savedStudent;
    }

    private record SchoolClassInfo(friasoft.gn.schoolapp.entity.school.SchoolClass schoolClass) {}

    private SchoolClassInfo loadSchoolClassAndAssertAccess(Long classId) {
        var sc = schoolClassRepository.findByIdWithYearAndSchool(classId)
            .orElseThrow(() -> new IllegalArgumentException("SchoolClass introuvable."));
        schoolService.assertCurrentUserCanAccessSchool(sc.getYear().getSchool().getId());
        return new SchoolClassInfo(sc);
    }

    private Parent resolveOrCreateParent(ParentRegistrationDTO parentDto, Long tenantId) {
        if (parentDto == null) {
            throw new IllegalArgumentException("Infos parent obligatoires.");
        }
        if (parentDto.phone() == null || parentDto.phone().isBlank()) {
            throw new IllegalArgumentException("phone parent obligatoire.");
        }
        String normalized = normalizePhone(parentDto.phone());
        return parentService.findByPhone(normalized)
            .orElseGet(() -> {
                Parent p = new Parent();
                p.setLastName(nonBlank(parentDto.lastName(), "Nom parent obligatoire."));
                p.setFirstName(nonBlank(parentDto.firstName(), "Prénom parent obligatoire."));
                p.setPhone(normalized);
                p.setEmail(trimToNull(parentDto.email()));
                p.setProfession(trimToNull(parentDto.profession()));
                p.setAddress(trimToNull(parentDto.address()));
                // TenantId injecté par ParentService.save()
                return parentService.save(p);
            });
    }

    private static Long requireTenantIdFromSecurity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Contexte utilisateur introuvable.");
        }
        Long tenantId = user.getTenantId();
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
        return String.valueOf(phone).trim().replaceAll("\\s+", "");
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


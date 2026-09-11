package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ParentDtos.ParentChildRow;
import friasoft.gn.schoolapp.dto.ParentDtos.ParentWriteRequest;
import friasoft.gn.schoolapp.dto.ParentSchoolListRow;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.Parent;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IParentRepository;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.util.GuineaContactValidation;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ParentService {

    private final IParentRepository parentRepository;
    private final IStudentRepository studentRepository;
    private final SchoolService schoolService;

    @Transactional(readOnly = true)
    public List<ParentSchoolListRow> listForSchoolActiveYear(Long schoolId) {
        if (schoolId == null) {
            throw new IllegalArgumentException("schoolId obligatoire.");
        }
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return parentRepository.listParentsWithEnrolledChildrenForSchoolActiveYear(schoolId);
    }

    @Transactional(readOnly = true)
    public Optional<Parent> findByPhone(String phone) {
        Long tenantId = requireTenantId();
        String normalized = normalizePhone(phone);
        return parentRepository.findByTenantIdAndPhone(tenantId, normalized);
    }

    @Transactional
    public Parent save(Parent parent) {
        Long tenantId = requireTenantId();
        parent.setTenantId(tenantId);
        GuineaContactValidation.requireValidEmail(parent.getEmail(), "Email");
        parent.setPhone(normalizePhone(parent.getPhone()));
        return parentRepository.save(parent);
    }

    @Transactional(readOnly = true)
    public Optional<Parent> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        Long tenantId = requireTenantId();
        return parentRepository.findById(id)
            .filter(p -> tenantId.equals(p.getTenantId()));
    }

    @Transactional(readOnly = true)
    public List<ParentChildRow> listChildren(Long parentId) {
        if (parentId == null) {
            return List.of();
        }
        List<Student> students = studentRepository.findAllByParentIdWithClass(parentId);
        Map<Long, ParentChildRow> byId = new LinkedHashMap<>();
        for (Student s : students) {
            byId.putIfAbsent(s.getId(), toChildRow(s, parentId));
        }
        return new ArrayList<>(byId.values());
    }

    @Transactional
    public Parent update(Long id, ParentWriteRequest body) {
        Parent parent = findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Parent introuvable."));
        String lastName = requireNonBlank(body.lastName(), "Nom obligatoire.");
        String firstName = requireNonBlank(body.firstName(), "Prénom obligatoire.");
        String normalized = normalizePhone(body.phone());
        Long tenantId = requireTenantId();
        parentRepository.findByTenantIdAndPhone(tenantId, normalized)
            .filter(other -> !other.getId().equals(parent.getId()))
            .ifPresent(other -> {
                throw new IllegalArgumentException("Ce numéro est déjà utilisé par un autre parent.");
            });
        GuineaContactValidation.requireValidEmail(body.email(), "Email");
        parent.setLastName(lastName);
        parent.setFirstName(firstName);
        parent.setPhone(normalized);
        parent.setEmail(trimToNull(body.email()));
        parent.setProfession(trimToNull(body.profession()));
        parent.setAddress(trimToNull(body.address()));
        return parentRepository.save(parent);
    }

    private static ParentChildRow toChildRow(Student s, Long parentId) {
        boolean asFather = s.getFather() != null && parentId.equals(s.getFather().getId());
        boolean asMother = s.getMother() != null && parentId.equals(s.getMother().getId());
        String relation;
        if (asFather && asMother) {
            relation = "PERE_ET_MERE";
        } else if (asFather) {
            relation = "PERE";
        } else {
            relation = "MERE";
        }
        String className = s.getSchoolClass() != null ? s.getSchoolClass().getName() : null;
        String status = s.getEnrollmentStatus() != null ? s.getEnrollmentStatus().name() : null;
        return new ParentChildRow(
            s.getId(),
            s.getFirstName(),
            s.getLastName(),
            s.getMatricule(),
            className,
            status,
            relation
        );
    }

    private static String requireNonBlank(String value, String message) {
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

    private static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Le numéro de téléphone est obligatoire.");
        }
        String compact = GuineaContactValidation.compactPhone(phone);
        GuineaContactValidation.requireValidGuineaPhone(compact, "Téléphone");
        return compact;
    }

    private static Long requireTenantId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user) || user.getTenantId() == null) {
            throw new IllegalStateException("Contexte tenant introuvable.");
        }
        return user.getTenantId();
    }
}

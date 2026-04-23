package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ParentSchoolListRow;
import friasoft.gn.schoolapp.dto.ParentDtos.ParentWriteRequest;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.Parent;
import friasoft.gn.schoolapp.repository.IParentRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ParentService {

    private final IParentRepository parentRepository;
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
        parent.setLastName(lastName);
        parent.setFirstName(firstName);
        parent.setPhone(normalized);
        parent.setEmail(trimToNull(body.email()));
        parent.setProfession(trimToNull(body.profession()));
        parent.setAddress(trimToNull(body.address()));
        return parentRepository.save(parent);
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
        return phone.trim().replaceAll("\\s+", "");
    }

    private static Long requireTenantId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user) || user.getTenantId() == null) {
            throw new IllegalStateException("Contexte tenant introuvable.");
        }
        return user.getTenantId();
    }
}

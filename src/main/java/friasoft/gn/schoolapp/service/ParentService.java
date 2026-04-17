package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.Parent;
import friasoft.gn.schoolapp.repository.IParentRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class ParentService {

    private final IParentRepository parentRepository;

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

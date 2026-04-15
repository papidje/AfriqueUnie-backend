package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.repository.ISchoolYearRepository;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SchoolYearService {

    private final ISchoolYearRepository repository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    @Transactional
    public SchoolYear save(SchoolYear year) {
        if (year.getSchool() == null || year.getSchool().getId() == null) {
            throw new IllegalArgumentException("L'école (school.id) est obligatoire.");
        }
        School school = schoolRepository.findById(year.getSchool().getId())
            .orElseThrow(() -> new IllegalArgumentException("École introuvable."));
        year.setSchool(school);
        year.setTenantId(school.getTenantId());

        User current = currentUserOrNull();
        if (current != null) {
            User actorRef = userRepository.getReferenceById(current.getId());
            if (year.getId() == null) {
                year.setCreatedBy(actorRef);
            }
            year.setUpdatedBy(actorRef);
        }

        if (year.isActive()) {
            repository.deactivateAllForSchool(school.getId());
        }

        return repository.save(year);
    }

    private static User currentUserOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }

    public Optional<SchoolYear> findById(Long id) {
        return repository.findById(id);
    }

    public List<SchoolYear> findBySchool(Long schoolId) {
        return repository.findBySchoolId(schoolId);
    }

    /** Année scolaire marquée {@code active} pour l’établissement donné (filtrée par tenant Hibernate si applicable). */
    public Optional<SchoolYear> getActiveYearForSchool(Long schoolId) {
        return repository.findFirstBySchoolIdAndActiveTrueOrderByIdDesc(schoolId);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}

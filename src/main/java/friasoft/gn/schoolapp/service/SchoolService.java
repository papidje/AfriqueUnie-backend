package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ClassSubjectDtos.TeacherSummaryResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    public List<School> listForAuthenticatedUser() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user.getRole() == User.UserRole.SUPER_ADMIN) {
            return getAll();
        }
        Long tenantId = user.getTenantId();
        if (tenantId == null) {
            return List.of();
        }
        return schoolRepository.findByTenantIdOrderByIdAsc(tenantId);
    }

    public void create(School school) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        school.setCreated_at(Instant.now());
        this.schoolRepository.save(school);
    }

    public List<School> getAll() {
        List<School> actualList = new ArrayList<>();
        this.schoolRepository.findAll().iterator().forEachRemaining(actualList::add);
        return actualList;
    }

    public School update(Long schoolId, School dto) {
        School school = this.schoolRepository.findById(schoolId).orElseThrow(() -> new RuntimeException("School not found"));
        school.setName(dto.getName());
        school.setAdress(dto.getAdress());
        school.setContact(dto.getContact());
        school.setLogo(dto.getLogo());
        school.setOpenDate(dto.getOpenDate());
        school.setUpdated_at(Instant.now());
        return this.schoolRepository.save(school);
    }

    public void delete(Long schoolId) {
        this.schoolRepository.deleteById(schoolId);
    }

    public void activate(Long schoolId, boolean active) {
        School school = this.schoolRepository.findById(schoolId).orElseThrow(() -> new RuntimeException("School not found"));
        school.setActive(active);
        this.schoolRepository.save(school);
    }

    public School getSchool(Long schoolId) {
        return this.schoolRepository.findById(schoolId).orElseThrow(() -> new RuntimeException("School not found"));
    }

    /**
     * Vérifie que l’utilisateur courant peut agir sur cette école (tenant JWT ou super admin).
     */
    @Transactional(readOnly = true)
    public List<TeacherSummaryResponse> listTeachersForSchool(Long schoolId) {
        assertCurrentUserCanAccessSchool(schoolId);
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "École introuvable."));
        return userRepository.findTeachersForSchool(schoolId, school.getTenantId()).stream()
            .map(u -> new TeacherSummaryResponse(u.getId(), u.getFullname(), u.getEmail()))
            .toList();
    }

    public void assertCurrentUserCanAccessSchool(Long schoolId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (user.getRole() == User.UserRole.SUPER_ADMIN) {
            return;
        }
        Long tenantId = user.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        boolean allowed = schoolRepository.findByTenantIdOrderByIdAsc(tenantId).stream()
            .anyMatch(s -> s.getId().equals(schoolId));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "École non accessible pour ce tenant");
        }
    }
}

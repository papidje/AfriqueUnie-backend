package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ClassSubjectDtos.TeacherSummaryResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import friasoft.gn.schoolapp.security.SchoolSecurity;
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
    private final SchoolSecurity schoolSecurity;

    public List<School> listForAuthenticatedUser() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user.getRole() == User.UserRole.SUPER_ADMIN) {
            return getAll();
        }
        if (user.getRole() == User.UserRole.DIRECTOR) {
            if (user.getSchool() == null || user.getSchool().getId() == null) {
                return List.of();
            }
            return schoolRepository.findById(user.getSchool().getId())
                .map(List::of)
                .orElse(List.of());
        }
        if (user.getRole() == User.UserRole.ADMIN_ECOLE) {
            Long tenantId = user.getOrganizationTenantId();
            if (tenantId == null) {
                tenantId = user.getTenantId();
            }
            if (tenantId == null) {
                return List.of();
            }
            return schoolRepository.findByTenantIdOrderByIdAsc(tenantId);
        }
        if (user.getRole() == User.UserRole.STAFF
            || user.getRole() == User.UserRole.TEACHER
            || user.getRole() == User.UserRole.ACCOUNTANT) {
            if (user.getSchool() == null || user.getSchool().getId() == null) {
                return List.of();
            }
            return schoolRepository.findById(user.getSchool().getId())
                .map(List::of)
                .orElse(List.of());
        }
        return List.of();
    }

    @Transactional
    public School create(School school) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Instant now = Instant.now();
        school.setId(null);
        school.setCreated_at(now);
        school.setUpdated_at(now);
        if (user.getRole() == User.UserRole.ADMIN_ECOLE) {
            Long tid = user.getOrganizationTenantId();
            if (tid == null) {
                tid = user.getTenantId();
            }
            if (tid == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant du compte introuvable.");
            }
            school.setTenantId(tid);
            school.setActive(false);
        } else if (user.getRole() == User.UserRole.SUPER_ADMIN) {
            // Le corps peut contenir tenant_id ; sinon création « sans tenant » (usage limité).
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return this.schoolRepository.save(school);
    }

    @Transactional
    public School updateLogoPath(Long schoolId, String logoPath) {
        School school = this.schoolRepository.findById(schoolId).orElseThrow(() -> new RuntimeException("School not found"));
        school.setLogo(logoPath);
        school.setUpdated_at(Instant.now());
        return this.schoolRepository.save(school);
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
        // Le logo est géré uniquement via l’upload (PATCH /schools/{id}/logo).
        school.setOpenDate(dto.getOpenDate());
        school.setUpdated_at(Instant.now());
        return this.schoolRepository.save(school);
    }

    @Transactional
    public void delete(Long schoolId) {
        assertCurrentUserCanAccessSchool(schoolId);
        this.schoolRepository.deleteById(schoolId);
    }

    @Transactional
    public void activate(Long schoolId, boolean active) {
        assertCurrentUserCanAccessSchool(schoolId);
        School school = this.schoolRepository.findById(schoolId).orElseThrow(() -> new RuntimeException("School not found"));
        school.setActive(active);
        school.setUpdated_at(Instant.now());
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
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "École introuvable."));
        if (!schoolSecurity.canAccessSchool(user, school)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "École non accessible pour ce compte.");
        }
    }
}

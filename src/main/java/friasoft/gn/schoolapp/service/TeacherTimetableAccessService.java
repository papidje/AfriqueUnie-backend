package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IClassTimetableSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Périmètre « enseignant » : classes visibles via au moins un créneau d’emploi du temps
 * dont la matière de classe est assignée à l’utilisateur (prof).
 */
@Service
@RequiredArgsConstructor
public class TeacherTimetableAccessService {

    private final IClassTimetableSlotRepository classTimetableSlotRepository;

    /**
     * @return empty si l’utilisateur n’est pas enseignant ; sinon l’ensemble des schoolClassId
     *         (peut être vide s’il n’a aucun créneau).
     */
    public Optional<Set<Long>> allowedSchoolClassIdsForCurrentUser() {
        User u = currentUser();
        if (u == null || u.getId() == null) {
            return Optional.empty();
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
            || auth.getAuthorities().stream().noneMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()))) {
            return Optional.empty();
        }
        return Optional.of(
            new HashSet<>(classTimetableSlotRepository.findDistinctSchoolClassIdsByTeacherUserId(u.getId()))
        );
    }

    public void assertCurrentTeacherCanAccessClassOrElseForbidden(Long classId) {
        if (classId == null) {
            return;
        }
        Optional<Set<Long>> allowed = allowedSchoolClassIdsForCurrentUser();
        if (allowed.isEmpty()) {
            return;
        }
        if (!allowed.get().contains(classId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès à cette classe refusé.");
        }
    }

    public void assertCurrentTeacherCanViewStudentOrElseForbidden(Student student) {
        Optional<Set<Long>> allowed = allowedSchoolClassIdsForCurrentUser();
        if (allowed.isEmpty()) {
            return;
        }
        if (student == null || student.getSchoolClass() == null || student.getSchoolClass().getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès à cet élève refusé.");
        }
        if (!allowed.get().contains(student.getSchoolClass().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès à cet élève refusé.");
        }
    }

    /** pour GET : pas d'exception, masque l'élève comme « introuvable » */
    public boolean isStudentVisibleInTeacherView(Student student) {
        Optional<Set<Long>> allowed = allowedSchoolClassIdsForCurrentUser();
        if (allowed.isEmpty()) {
            return true;
        }
        if (student.getSchoolClass() == null || student.getSchoolClass().getId() == null) {
            return false;
        }
        return allowed.get().contains(student.getSchoolClass().getId());
    }

    private static User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            return null;
        }
        return u;
    }
}

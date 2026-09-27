package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.AcceptRequest;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.AddCommentRequest;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.CommentResponse;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.CreateRequest;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.RefuseRequest;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.RequestDetail;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.RequestSummary;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.Subject;
import friasoft.gn.schoolapp.entity.school.SubjectAdditionRequest;
import friasoft.gn.schoolapp.entity.school.SubjectAdditionRequestComment;
import friasoft.gn.schoolapp.entity.school.SubjectAdditionRequestStatus;
import friasoft.gn.schoolapp.repository.IClassLevelRepository;
import friasoft.gn.schoolapp.repository.ISubjectAdditionRequestCommentRepository;
import friasoft.gn.schoolapp.repository.ISubjectAdditionRequestRepository;
import friasoft.gn.schoolapp.repository.UserPlatformRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubjectAdditionRequestService {

    private final ISubjectAdditionRequestRepository requestRepository;
    private final ISubjectAdditionRequestCommentRepository commentRepository;
    private final IClassLevelRepository classLevelRepository;
    private final SchoolService schoolService;
    private final SubjectService subjectService;
    private final InAppNotificationService inAppNotificationService;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final UserCapabilityService userCapabilityService;

    @Transactional
    public RequestDetail create(Long schoolId, CreateRequest body) {
        User current = requireCurrentUser();
        School school = schoolService.getSchool(schoolId);
        schoolService.assertCurrentUserCanAccessSchool(schoolId);

        String name = requireNonBlank(body != null ? body.subjectName() : null, "Le nom de la matière est obligatoire.");
        if (name.length() > 200) {
            throw new IllegalArgumentException("Le nom ne peut pas dépasser 200 caractères.");
        }
        String comment = requireNonBlank(body != null ? body.comment() : null, "Le commentaire de justification est obligatoire.");
        if (body.classLevelId() == null) {
            throw new IllegalArgumentException("Le niveau scolaire est obligatoire.");
        }
        ClassLevel level = classLevelRepository.findByIdWithGroup(body.classLevelId())
            .orElseThrow(() -> new IllegalArgumentException("Niveau introuvable."));

        SubjectAdditionRequest request = new SubjectAdditionRequest();
        request.setTenantId(school.getTenantId());
        request.setSchool(school);
        request.setRequestedBy(current);
        request.setSubjectName(name);
        request.setClassLevel(level);
        request.setStatus(SubjectAdditionRequestStatus.OPEN);
        SubjectAdditionRequest saved = requestRepository.save(request);

        SubjectAdditionRequestComment initial = new SubjectAdditionRequestComment();
        initial.setRequest(saved);
        initial.setAuthor(current);
        initial.setBody(comment.trim());
        commentRepository.save(initial);

        notifySuperAdmins(
            "Demande d’ajout de matière",
            "« " + name + " » demandée par " + displayName(current) + " (" + school.getName() + ").",
            saved.getId()
        );

        return toDetail(saved);
    }

    @Transactional(readOnly = true)
    public List<RequestSummary> listMine(String filter) {
        User current = requireCurrentUser();
        List<SubjectAdditionRequest> rows = resolveFilter(filter) == null
            ? requestRepository.findAllByRequesterWithRefs(current.getId())
            : requestRepository.findAllByRequesterAndStatusInWithRefs(current.getId(), resolveFilter(filter));
        return toSummaries(rows);
    }

    @Transactional(readOnly = true)
    public List<RequestSummary> listForSuperAdmin(String filter) {
        assertSuperAdmin();
        List<SubjectAdditionRequest> rows = resolveFilter(filter) == null
            ? requestRepository.findAllWithRefs()
            : requestRepository.findAllByStatusInWithRefs(resolveFilter(filter));
        return toSummaries(rows);
    }

    @Transactional(readOnly = true)
    public RequestDetail getDetail(Long id) {
        SubjectAdditionRequest request = loadVisible(id);
        return toDetail(request);
    }

    @Transactional
    public CommentResponse addComment(Long id, AddCommentRequest body) {
        User current = requireCurrentUser();
        SubjectAdditionRequest request = loadVisible(id);
        if (request.getStatus() != SubjectAdditionRequestStatus.OPEN) {
            throw new IllegalArgumentException("Impossible de commenter une demande fermée.");
        }
        String text = requireNonBlank(body != null ? body.body() : null, "Le commentaire est obligatoire.");
        SubjectAdditionRequestComment comment = new SubjectAdditionRequestComment();
        comment.setRequest(request);
        comment.setAuthor(current);
        comment.setBody(text.trim());
        SubjectAdditionRequestComment saved = commentRepository.save(comment);
        request.setUpdatedAt(LocalDateTime.now());
        requestRepository.save(request);

        boolean authorIsSa = userCapabilityService.isSuperAdmin(current);
        if (authorIsSa) {
            notifyUser(
                request.getRequestedBy(),
                request.getSchool(),
                "Réponse sur votre demande de matière",
                "Nouveau commentaire sur « " + request.getSubjectName() + " ».",
                request.getId()
            );
        } else {
            notifySuperAdmins(
                "Commentaire sur une demande de matière",
                displayName(current) + " a commenté « " + request.getSubjectName() + " ».",
                request.getId()
            );
        }
        return toComment(saved, authorIsSa);
    }

    @Transactional
    public RequestDetail accept(Long id, AcceptRequest body) {
        User current = requireCurrentUser();
        assertSuperAdmin();
        SubjectAdditionRequest request = requestRepository.findByIdWithRefs(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        if (request.getStatus() != SubjectAdditionRequestStatus.OPEN) {
            throw new IllegalArgumentException("Cette demande est déjà fermée.");
        }
        String code = requireNonBlank(body != null ? body.code() : null, "Le code matière est obligatoire.");
        String name = body.name() != null && !body.name().isBlank()
            ? body.name().trim()
            : request.getSubjectName();
        List<String> groupCodes = body.levelGroupCodes();
        if (groupCodes == null || groupCodes.isEmpty()) {
            ClassLevelGroup g = request.getClassLevel().getGroup();
            groupCodes = g != null && g.getCode() != null ? List.of(g.getCode()) : List.of();
        }
        Subject created;
        try {
            created = subjectService.createGlobal(code, name, groupCodes);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        request.setStatus(SubjectAdditionRequestStatus.ACCEPTED);
        request.setCreatedSubject(created);
        request.setClosedAt(LocalDateTime.now());
        request.setClosedBy(current);
        requestRepository.save(request);

        notifyUser(
            request.getRequestedBy(),
            request.getSchool(),
            "Demande de matière acceptée",
            "« " + name + " » (" + created.getCode() + ") a été ajoutée au référentiel.",
            request.getId()
        );
        return toDetail(request);
    }

    @Transactional
    public RequestDetail refuse(Long id, RefuseRequest body) {
        User current = requireCurrentUser();
        assertSuperAdmin();
        SubjectAdditionRequest request = requestRepository.findByIdWithRefs(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        if (request.getStatus() != SubjectAdditionRequestStatus.OPEN) {
            throw new IllegalArgumentException("Cette demande est déjà fermée.");
        }
        if (body != null && body.comment() != null && !body.comment().isBlank()) {
            SubjectAdditionRequestComment comment = new SubjectAdditionRequestComment();
            comment.setRequest(request);
            comment.setAuthor(current);
            comment.setBody(body.comment().trim());
            commentRepository.save(comment);
        }
        request.setStatus(SubjectAdditionRequestStatus.REFUSED);
        request.setClosedAt(LocalDateTime.now());
        request.setClosedBy(current);
        requestRepository.save(request);

        notifyUser(
            request.getRequestedBy(),
            request.getSchool(),
            "Demande de matière refusée",
            "La demande « " + request.getSubjectName() + " » a été refusée.",
            request.getId()
        );
        return toDetail(request);
    }

    private SubjectAdditionRequest loadVisible(Long id) {
        SubjectAdditionRequest request = requestRepository.findByIdWithRefs(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        User current = requireCurrentUser();
        if (userCapabilityService.isSuperAdmin(current)) {
            return request;
        }
        if (!current.getId().equals(request.getRequestedBy().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à cette demande.");
        }
        schoolService.assertCurrentUserCanAccessSchool(request.getSchool().getId());
        return request;
    }

    private Collection<SubjectAdditionRequestStatus> resolveFilter(String filter) {
        if (filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter.trim())) {
            return null;
        }
        String f = filter.trim().toUpperCase();
        if ("OPEN".equals(f) || "OUVERTE".equals(f) || "OUVERTES".equals(f)) {
            return Set.of(SubjectAdditionRequestStatus.OPEN);
        }
        if ("CLOSED".equals(f) || "FERMEE".equals(f) || "FERMEES".equals(f) || "FERMÉE".equals(f) || "FERMÉES".equals(f)) {
            return Set.of(SubjectAdditionRequestStatus.ACCEPTED, SubjectAdditionRequestStatus.REFUSED);
        }
        if ("ACCEPTED".equals(f) || "REFUSED".equals(f)) {
            return Set.of(SubjectAdditionRequestStatus.valueOf(f));
        }
        throw new IllegalArgumentException("Filtre invalide (OPEN, CLOSED ou ALL).");
    }

    private List<RequestSummary> toSummaries(List<SubjectAdditionRequest> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(SubjectAdditionRequest::getId).toList();
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : commentRepository.countByRequestIds(ids)) {
            counts.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return rows.stream().map(r -> toSummary(r, counts.getOrDefault(r.getId(), 0))).toList();
    }

    private RequestSummary toSummary(SubjectAdditionRequest r, int commentCount) {
        ClassLevel lv = r.getClassLevel();
        ClassLevelGroup g = lv != null ? lv.getGroup() : null;
        return new RequestSummary(
            r.getId(),
            r.getSubjectName(),
            r.getStatus().name(),
            r.getSchool().getId(),
            r.getSchool().getName(),
            lv != null ? lv.getId() : null,
            lv != null ? lv.getCode() : null,
            lv != null ? lv.getName() : null,
            g != null ? g.getCode() : null,
            g != null ? g.getName() : null,
            r.getRequestedBy().getId(),
            displayName(r.getRequestedBy()),
            r.getCreatedAt(),
            r.getUpdatedAt(),
            r.getClosedAt(),
            commentCount
        );
    }

    private RequestDetail toDetail(SubjectAdditionRequest r) {
        List<SubjectAdditionRequestComment> comments =
            commentRepository.findByRequestIdOrderByCreatedAtAsc(r.getId());
        ClassLevel lv = r.getClassLevel();
        ClassLevelGroup g = lv != null ? lv.getGroup() : null;
        Subject created = r.getCreatedSubject();
        return new RequestDetail(
            r.getId(),
            r.getSubjectName(),
            r.getStatus().name(),
            r.getSchool().getId(),
            r.getSchool().getName(),
            lv != null ? lv.getId() : null,
            lv != null ? lv.getCode() : null,
            lv != null ? lv.getName() : null,
            g != null ? g.getCode() : null,
            g != null ? g.getName() : null,
            r.getRequestedBy().getId(),
            displayName(r.getRequestedBy()),
            r.getCreatedAt(),
            r.getUpdatedAt(),
            r.getClosedAt(),
            created != null ? created.getId() : null,
            created != null ? created.getCode() : null,
            comments.stream().map(c -> toComment(c, userCapabilityService.isSuperAdmin(c.getAuthor()))).toList()
        );
    }

    private static CommentResponse toComment(SubjectAdditionRequestComment c, boolean authorIsSuperAdmin) {
        return new CommentResponse(
            c.getId(),
            c.getAuthor().getId(),
            displayName(c.getAuthor()),
            authorIsSuperAdmin,
            c.getBody(),
            c.getCreatedAt()
        );
    }

    private void notifySuperAdmins(String title, String content, Long requestId) {
        for (User sa : userPlatformRoleRepository.findActiveUsersByRole(User.UserRole.SUPER_ADMIN)) {
            notifyUser(sa, null, title, content, requestId);
        }
    }

    private void notifyUser(User user, School school, String title, String content, Long requestId) {
        inAppNotificationService.createUserTargetedNotification(user, school, title, content, requestId);
    }

    private void assertSuperAdmin() {
        if (!userCapabilityService.isSuperAdmin(requireCurrentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé au super administrateur.");
        }
    }

    private static User requireCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Contexte utilisateur introuvable.");
        }
        return user;
    }

    private static String displayName(User user) {
        if (user == null) {
            return "—";
        }
        String n = user.getFullname();
        if (n != null && !n.isBlank()) {
            return n.trim();
        }
        return user.getEmail() != null ? user.getEmail() : ("#" + user.getId());
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}

package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.notification.MarkAllReadResponse;
import friasoft.gn.schoolapp.dto.notification.NotificationResponse;
import friasoft.gn.schoolapp.dto.notification.UnreadNotificationCountResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.service.InAppNotificationService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("notifications")
@AllArgsConstructor
public class NotificationController {

    private final InAppNotificationService inAppNotificationService;

    /**
     * Liste les notifications visibles pour l’utilisateur (voir ciblage).
     *
     * @param unreadOnly si {@code true}, limite aux notifications sans état « lu ».
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<NotificationResponse> list(
        @AuthenticationPrincipal User principal,
        @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly
    ) {
        Long tenantId =
            principal.getOrganizationTenantId() != null ? principal.getOrganizationTenantId() : principal.getTenantId();
        return inAppNotificationService.listForUser(principal, unreadOnly, tenantId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse unreadCount(@AuthenticationPrincipal User principal) {
        Long tenantId =
            principal.getOrganizationTenantId() != null ? principal.getOrganizationTenantId() : principal.getTenantId();
        return new UnreadNotificationCountResponse(inAppNotificationService.countUnread(principal, tenantId));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismiss(@AuthenticationPrincipal User principal, @PathVariable("id") Long notificationId) {
        Long tenantId =
            principal.getOrganizationTenantId() != null ? principal.getOrganizationTenantId() : principal.getTenantId();
        inAppNotificationService.dismissForUser(principal, notificationId, tenantId);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/mark-all-read")
    public MarkAllReadResponse markAllRead(@AuthenticationPrincipal User principal) {
        Long tenantId =
            principal.getOrganizationTenantId() != null ? principal.getOrganizationTenantId() : principal.getTenantId();
        return inAppNotificationService.markAllReadForUser(principal, tenantId);
    }
}

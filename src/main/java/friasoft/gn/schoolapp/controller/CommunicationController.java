package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.communication.CommunicationDtos.CommunicationBatchSettingsResponse;
import friasoft.gn.schoolapp.dto.communication.CommunicationDtos.CommunicationBatchSettingsUpdateRequest;
import friasoft.gn.schoolapp.dto.communication.CommunicationDtos.CommunicationHistoryRow;
import friasoft.gn.schoolapp.dto.communication.CommunicationDtos.CommunicationManualSendRequest;
import friasoft.gn.schoolapp.dto.communication.CommunicationDtos.CommunicationManualSendResponse;
import friasoft.gn.schoolapp.dto.communication.CommunicationDtos.CommunicationScheduledPreviewRow;
import friasoft.gn.schoolapp.entity.school.NotificationBatchSettings;
import friasoft.gn.schoolapp.entity.school.NotificationDeliveryHistory;
import friasoft.gn.schoolapp.repository.INotificationDeliveryHistoryRepository;
import friasoft.gn.schoolapp.service.communication.CommunicationManualNotificationService;
import friasoft.gn.schoolapp.service.communication.CommunicationPreviewService;
import friasoft.gn.schoolapp.service.communication.NotificationBatchSettingsService;
import friasoft.gn.schoolapp.service.SchoolService;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/communication")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_ECOLE','DIRECTOR','STAFF')")
public class CommunicationController {

    private final NotificationBatchSettingsService notificationBatchSettingsService;
    private final CommunicationPreviewService communicationPreviewService;
    private final CommunicationManualNotificationService communicationManualNotificationService;
    private final INotificationDeliveryHistoryRepository notificationDeliveryHistoryRepository;
    private final SchoolService schoolService;

    @GetMapping("/settings")
    public CommunicationBatchSettingsResponse getSettings() {
        Long tenantId = requireTenant();
        NotificationBatchSettings s = notificationBatchSettingsService.resolveForTenant(tenantId);
        return CommunicationPreviewService.toResponse(s);
    }

    @PutMapping("/settings")
    public CommunicationBatchSettingsResponse updateSettings(@RequestBody CommunicationBatchSettingsUpdateRequest body) {
        Long tenantId = requireTenant();
        NotificationBatchSettings s = notificationBatchSettingsService.resolveForTenant(tenantId);
        CommunicationPreviewService.applyUpdate(s, body);
        return CommunicationPreviewService.toResponse(notificationBatchSettingsService.save(s));
    }

    @GetMapping("/scheduled-preview")
    public List<CommunicationScheduledPreviewRow> scheduledPreview(@RequestParam Long schoolId) {
        requireTenant();
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return communicationPreviewService.previewScheduledForSchool(schoolId);
    }

    @GetMapping("/history")
    public Page<CommunicationHistoryRow> history(
        @RequestParam Long schoolId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        requireTenant();
        schoolService.assertCurrentUserCanAccessSchool(schoolId);
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(Math.max(5, size), 100));
        return notificationDeliveryHistoryRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, p).map(CommunicationController::mapHistory);
    }

    @PostMapping("/manual-send")
    public CommunicationManualSendResponse manualSend(@RequestBody CommunicationManualSendRequest body) {
        Long tenantId = requireTenant();
        try {
            return communicationManualNotificationService.dispatch(tenantId, body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static Long requireTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant manquant.");
        }
        return tenantId;
    }

    private static CommunicationHistoryRow mapHistory(NotificationDeliveryHistory h) {
        return new CommunicationHistoryRow(
            h.getId(),
            h.getCreatedAt(),
            h.getSource().name(),
            h.getEventType(),
            h.getStatus().name(),
            h.getChannel().name(),
            h.getTitle(),
            h.getRecipientsSummary(),
            h.getBodyPreview(),
            h.getBodyContent(),
            h.getErrorMessage()
        );
    }
}

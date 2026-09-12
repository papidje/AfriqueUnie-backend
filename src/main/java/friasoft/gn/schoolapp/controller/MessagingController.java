package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.messaging.MessagingConversationDto;
import friasoft.gn.schoolapp.dto.messaging.MessagingMessageDto;
import friasoft.gn.schoolapp.dto.messaging.MessagingUnreadSummaryDto;
import friasoft.gn.schoolapp.dto.messaging.MessagingUserSummaryDto;
import friasoft.gn.schoolapp.dto.messaging.SendMessageRequest;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.service.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("messaging")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MessagingController {

    private final MessagingService messagingService;

    @GetMapping("/unread-summary")
    public MessagingUnreadSummaryDto unreadSummary(@AuthenticationPrincipal User principal) {
        return messagingService.unreadSummary(principal);
    }

    @GetMapping("/conversations")
    public List<MessagingConversationDto> conversations(
        @AuthenticationPrincipal User principal,
        @RequestParam(value = "q", required = false) String q
    ) {
        return messagingService.listConversations(principal, q);
    }

    @GetMapping("/contacts")
    public List<MessagingUserSummaryDto> contacts(
        @AuthenticationPrincipal User principal,
        @RequestParam(value = "q", required = false) String q
    ) {
        return messagingService.listContacts(principal, q);
    }

    @PostMapping("/conversations/with/{userId}")
    public MessagingConversationDto getOrCreateWith(
        @AuthenticationPrincipal User principal,
        @PathVariable Long userId
    ) {
        return messagingService.getOrCreateWith(principal, userId);
    }

    @GetMapping("/conversations/{id}/messages")
    public List<MessagingMessageDto> messages(
        @AuthenticationPrincipal User principal,
        @PathVariable Long id,
        @RequestParam(value = "afterId", required = false) Long afterId,
        @RequestParam(value = "beforeId", required = false) Long beforeId,
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return messagingService.listMessages(principal, id, afterId, beforeId, limit);
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessagingMessageDto send(
        @AuthenticationPrincipal User principal,
        @PathVariable Long id,
        @RequestBody SendMessageRequest request
    ) {
        return messagingService.sendMessage(principal, id, request);
    }

    @PostMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal User principal, @PathVariable Long id) {
        messagingService.markRead(principal, id);
    }
}

package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.ChangePasswordRequest;
import friasoft.gn.schoolapp.dto.InviteUserDTO;
import friasoft.gn.schoolapp.dto.InviteUserResponse;
import friasoft.gn.schoolapp.dto.OwnProfileUpdateRequest;
import friasoft.gn.schoolapp.dto.ProfileSchoolSummary;
import friasoft.gn.schoolapp.dto.UpdateUserAffiliationsRequest;
import friasoft.gn.schoolapp.dto.UserAffiliationResponse;
import friasoft.gn.schoolapp.dto.UserProfileResponse;
import friasoft.gn.schoolapp.dto.UserResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.service.SchoolService;
import friasoft.gn.schoolapp.service.UserAffiliationInviteActionService;
import friasoft.gn.schoolapp.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("users")
public class UserController {
    private UserService userService;
    private SchoolService schoolService;
    private UserAffiliationInviteActionService userAffiliationInviteActionService;

    @PreAuthorize("@userMgmtSecurity.canManageDirectory(authentication)")
    @GetMapping()
    public List<UserResponse> getAll() {
        User viewer = this.userService.getUserInfo();
        List<User> users = this.userService.getAll();
        Set<Long> ids = users.stream().map(User::getId).collect(Collectors.toSet());
        Long tenantId = this.userService.resolveDirectoryTenantId(viewer);
        Set<Long> privacyMask = this.userService.userIdsRequiringDirectoryPrivacyMask(ids, tenantId);
        Map<Long, List<UserAffiliationResponse>> affMap =
            tenantId != null
                ? this.userService.getDirectoryAffiliationSummariesGrouped(ids, tenantId)
                : this.userService.getActiveAffiliationSummariesGrouped(ids);
        return users.stream().map(u -> this.mapToDirectoryUserResponse(u, affMap, privacyMask)).toList();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/userInfo")
    public UserProfileResponse getUserInfo() {
        return this.mapToUserProfileResponse(this.userService.getUserInfo());
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/profile")
    public UserProfileResponse updateOwnProfile(@RequestBody OwnProfileUpdateRequest body) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String fullname = body != null ? body.fullname() : null;
        User updated = this.userService.updateOwnProfile(user, fullname);
        return this.mapToUserProfileResponse(updated);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest body) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        this.userService.changeOwnPassword(user, body.currentPassword(), body.newPassword());
    }

    @PreAuthorize("@userMgmtSecurity.canManageDirectory(authentication)")
    @GetMapping("/admins-by-school/{schoolId}")
    public List<UserResponse> getAdminsBySchool(@PathVariable Long schoolId) {
        this.schoolService.assertCurrentUserCanAccessSchool(schoolId);
        List<User> users = this.userService.getAdminBySchool(schoolId);
        Set<Long> ids = users.stream().map(User::getId).collect(Collectors.toSet());
        Long tenantId = this.schoolService.getSchool(schoolId).getTenantId();
        Set<Long> privacyMask = this.userService.userIdsRequiringDirectoryPrivacyMask(ids, tenantId);
        Map<Long, List<UserAffiliationResponse>> affMap =
            tenantId != null
                ? this.userService.getDirectoryAffiliationSummariesGrouped(ids, tenantId)
                : this.userService.getActiveAffiliationSummariesGrouped(ids);
        return users.stream().map(u -> this.mapToDirectoryUserResponse(u, affMap, privacyMask)).toList();
    }

    @PreAuthorize("@userMgmtSecurity.canManageDirectory(authentication)")
    @GetMapping("/search-admins/{term}")
    public List<UserResponse> getAdminsBySchool(@PathVariable String term) {
        User viewer = this.userService.getUserInfo();
        List<User> users = this.userService.getUnassignedUsers(term);
        Set<Long> ids = users.stream().map(User::getId).collect(Collectors.toSet());
        Long tenantId = this.userService.resolveDirectoryTenantId(viewer);
        Set<Long> privacyMask = this.userService.userIdsRequiringDirectoryPrivacyMask(ids, tenantId);
        Map<Long, List<UserAffiliationResponse>> affMap =
            tenantId != null
                ? this.userService.getDirectoryAffiliationSummariesGrouped(ids, tenantId)
                : this.userService.getActiveAffiliationSummariesGrouped(ids);
        return users.stream().map(u -> this.mapToDirectoryUserResponse(u, affMap, privacyMask)).toList();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@userMgmtSecurity.canManageDirectory(authentication)")
    @PostMapping("/invite")
    public InviteUserResponse inviteUser(@RequestBody InviteUserDTO body) {
        return this.userService.inviteUser(body);
    }

    @PatchMapping("/{userId}/affiliations")
    @PreAuthorize("@userMgmtSecurity.canManageAffiliationsAsFounder(authentication)")
    public UserResponse patchUserAffiliations(
        @PathVariable Long userId,
        @RequestBody UpdateUserAffiliationsRequest body
    ) {
        User updated = this.userService.syncUserAffiliations(userId, body);
        Map<Long, List<UserAffiliationResponse>> affMap =
            this.userService.getActiveAffiliationSummariesGrouped(Set.of(updated.getId()));
        return mapToDirectoryUserResponse(updated, affMap, Set.of());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{userId}/schools/{schoolId}/suspend")
    @PreAuthorize("@userMgmtSecurity.canManageAffiliationsAsFounder(authentication)")
    public void suspendAffiliationAtSchool(@PathVariable Long userId, @PathVariable Long schoolId) {
        this.userService.suspendAffiliationAtSchool(userId, schoolId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{userId}/schools/{schoolId}/reactivate")
    @PreAuthorize("@userMgmtSecurity.canManageAffiliationsAsFounder(authentication)")
    public void reactivateAffiliationAtSchool(@PathVariable Long userId, @PathVariable Long schoolId) {
        this.userService.reactivateAffiliationAtSchool(userId, schoolId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@userMgmtSecurity.canManageDirectory(authentication)")
    @PostMapping("/{userId}/resend-activation")
    public void resendActivation(@PathVariable Long userId) {
        this.userService.resendActivationEmail(userId);
    }

    @PostMapping("/affiliations/{affiliationId}/accept")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> acceptAffiliation(
        @PathVariable Long affiliationId,
        @AuthenticationPrincipal User principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return this.userAffiliationInviteActionService.acceptInvitation(affiliationId, principal);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/affiliations/{affiliationId}/refuse")
    @PreAuthorize("isAuthenticated()")
    public void refuseAffiliation(
        @PathVariable Long affiliationId,
        @AuthenticationPrincipal User principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        this.userAffiliationInviteActionService.refuseInvitation(affiliationId, principal);
    }

    private UserResponse mapToDirectoryUserResponse(
        User user,
        Map<Long, List<UserAffiliationResponse>> affMap,
        Set<Long> privacyMaskUserIds
    ) {
        return mapToDirectoryUserResponse(
            user,
            affMap.getOrDefault(user.getId(), List.of()),
            privacyMaskUserIds.contains(user.getId())
        );
    }

    private UserResponse mapToDirectoryUserResponse(
        User user,
        List<UserAffiliationResponse> tenantAffiliations,
        boolean privacyMask
    ) {
        List<String> effectiveRoles = privacyMask
            ? tenantAffiliations.stream().map(UserAffiliationResponse::role).distinct().sorted().toList()
            : userService.resolveEffectiveRoleNamesForProfile(user);
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            privacyMask ? null : user.getFullname(),
            user.getEmail(),
            user.isActive(),
            effectiveRoles,
            tenantAffiliations,
            privacyMask ? "En attente d'acceptation" : null);
    }

    /**
     * Profil connecté : écoles accessibles, affiliations actives par établissement, et rôle effectif du contexte courant.
     */
    private UserProfileResponse mapToUserProfileResponse(User user) {
        List<String> effectiveRoles = userService.resolveEffectiveRoleNamesForProfile(user);
        List<ProfileSchoolSummary> schools = this.schoolService.listForAuthenticatedUser().stream()
            .map(s -> new ProfileSchoolSummary(
                s.getId(),
                (s.getName() != null && !s.getName().isBlank())
                    ? s.getName().trim()
                    : ("Établissement #" + s.getId())
            ))
            .toList();
        Map<Long, List<UserAffiliationResponse>> affMap =
            this.userService.getActiveAffiliationSummariesGrouped(Set.of(user.getId()));
        List<UserAffiliationResponse> activeAffiliations =
            affMap.getOrDefault(user.getId(), List.of());
        return new UserProfileResponse(
            user.getUsername(),
            user.getFullname(),
            user.getEmail(),
            user.isActive(),
            effectiveRoles,
            user.getLastLoginAt(),
            schools,
            activeAffiliations
        );
    }
}

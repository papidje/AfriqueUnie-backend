package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.ChangePasswordRequest;
import friasoft.gn.schoolapp.dto.InviteUserDTO;
import friasoft.gn.schoolapp.dto.InviteUserResponse;
import friasoft.gn.schoolapp.dto.UserResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.service.SchoolService;
import friasoft.gn.schoolapp.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("users")
public class UserController {
    private UserService userService;
    private SchoolService schoolService;

    @GetMapping()
    public List<UserResponse> getAll() {
        return this.userService
            .getAll()
            .stream()
            .map(this::mapToUserResponse)
            .toList();
    }

    @GetMapping("/userInfo")
    public UserResponse getUserInfo() {
        return this.mapToUserResponse(this.userService.getUserInfo());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest body) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        this.userService.changeOwnPassword(user, body.currentPassword(), body.newPassword());
    }

    @GetMapping("/admins-by-school/{schoolId}")
    public List<UserResponse> getAdminsBySchool(@PathVariable Long schoolId) {
        this.schoolService.assertCurrentUserCanAccessSchool(schoolId);
        return this
            .userService.getAdminBySchool(schoolId)
            .stream()
            .map(this::mapToUserResponse)
            .toList();
    }

    @GetMapping("/search-admins/{term}")
    public List<UserResponse> getAdminsBySchool(@PathVariable String term) {
        return this
            .userService.getUnassignedUsers(term)
            .stream()
            .map(this::mapToUserResponse)
            .toList();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/invite")
    public InviteUserResponse inviteUser(@RequestBody InviteUserDTO body) {
        String code = this.userService.inviteUser(body);
        return new InviteUserResponse("Utilisateur invité avec succès", code);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{userId}/resend-activation")
    public void resendActivation(@PathVariable Long userId) {
        this.userService.resendActivationEmail(userId);
    }

    private UserResponse mapToUserResponse(User user) {
        List<String> effectiveRoles = List.of(
            (user.getRole() != null ? user.getRole() : User.UserRole.STAFF).name()
        );
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getFullname(),
            user.getEmail(),
            user.isActive(),
            effectiveRoles);
    }
}

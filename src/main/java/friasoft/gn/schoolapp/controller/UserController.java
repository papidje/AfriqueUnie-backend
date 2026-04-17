package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.InviteUserDTO;
import friasoft.gn.schoolapp.dto.InviteUserResponse;
import friasoft.gn.schoolapp.dto.UserResponse;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("users")
public class UserController {
    private UserService userService;

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

    @GetMapping("/admins-by-school/{schoolId}")
    public List<UserResponse> getAdminsBySchool(@PathVariable Long schoolId) {
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

package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.UserRequest;
import friasoft.gn.schoolapp.dto.UserResponse;
import friasoft.gn.schoolapp.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return this.userService.getAll().stream().map(
            user -> new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullname(),
                user.getEmail(),
                user.isActive(),
                user.getRoles().stream().map(role -> role.getName().name()).toList()
            )
        ).toList();
    }

    @PostMapping("registery")
    public void registery(@RequestBody UserRequest user) {
        log.info("Registery");
        this.userService.registery(user);
    }
}

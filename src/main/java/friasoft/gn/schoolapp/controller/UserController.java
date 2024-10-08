package friasoft.gn.schoolapp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import friasoft.gn.schoolapp.dto.ActivationRequest;
import friasoft.gn.schoolapp.dto.LoginRequest;
import friasoft.gn.schoolapp.dto.UserRequest;
import friasoft.gn.schoolapp.dto.UserResponse;
import friasoft.gn.schoolapp.security.JwtService;
import friasoft.gn.schoolapp.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("users")
public class UserController {
    private UserService userService;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    
    @PostMapping("registery")
    public void registery(@RequestBody UserRequest user) {
        log.info("Registery");
        this.userService.registery(user);
    }

    @GetMapping()
    public List<UserResponse> getAll() {
        return this.userService.getAll().stream().map(
            user -> new UserResponse(
                user.getId(), 
                user.getName(), 
                user.getEmail(), 
                user.isActive(),
                user.getRole().getName().name()
            )
        ).toList();
    }

    @PostMapping(path = "activate")
    public void activate(@RequestBody ActivationRequest activation) {
        this.userService.activate(activation);
    }

    @PostMapping(path = "login")
    public Map<String, String> login(@RequestBody LoginRequest request) {
        final Authentication authentication = this.authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.userName(), request.password())
        );
        log.info("resultat {}", authentication);
        if (authentication.isAuthenticated()) {
            return this.jwtService.generate(request.userName());
        }
        return null;
    }
}

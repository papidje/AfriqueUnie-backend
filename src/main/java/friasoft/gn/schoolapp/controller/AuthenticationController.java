package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.ActivationRequest;
import friasoft.gn.schoolapp.dto.LoginRequest;
import friasoft.gn.schoolapp.dto.RegistrationRequest;
import friasoft.gn.schoolapp.security.JwtService;
import friasoft.gn.schoolapp.service.RegistrationService;
import friasoft.gn.schoolapp.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("auth")
public class AuthenticationController {
    private UserService userService;
    private RegistrationService registrationService;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;

    @PostMapping(path = "activate")
    public void activate(@RequestBody ActivationRequest activation) {
        this.userService.activate(activation);
    }

    @PostMapping(path = "reset-password")
    public void resetPassword(@RequestBody Map<String, String> resetPasswordRequest) {
        this.userService.resetPassword(resetPasswordRequest);
    }

    @PostMapping(path = "new-password")
    public void updatePassword(@RequestBody Map<String, String> updatePasswordRequest) {
        this.userService.updatePassword(updatePasswordRequest);
    }

    @PostMapping(path = "refresh-token")
    public @ResponseBody Map<String, String> refreshToken(@RequestBody Map<String, String> refreshTokenRequest) {
        return this.jwtService.refreshToken(refreshTokenRequest);
    }

    @PostMapping(path = "login")
    public Map<String, String> login(@RequestBody LoginRequest request) {
        final Authentication authentication = this.authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.userName(), request.password())
        );
        if (authentication.isAuthenticated()) {
            return this.jwtService.generate(request.userName(), true);
        }
        return null;
    }

    @PostMapping(path = "logout")
    public void logout() {
        this.jwtService.logout();
    }

    @PostMapping(path = "register-school-admin")
    public void registerSchoolAdmin(@RequestBody RegistrationRequest request) {
        this.registrationService.registerSchoolAdmin(request);
    }

}

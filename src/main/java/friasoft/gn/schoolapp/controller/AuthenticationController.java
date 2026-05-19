package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.ActivationRequest;
import friasoft.gn.schoolapp.dto.LoginRequest;
import friasoft.gn.schoolapp.dto.RegistrationRequest;
import friasoft.gn.schoolapp.dto.SwitchSchoolRequest;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.security.JwtService;
import friasoft.gn.schoolapp.service.RegistrationService;
import friasoft.gn.schoolapp.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Régénère access + refresh JWT pour l’établissement actif (claims {@code school_id} et {@code roles}
     * alignés sur l’affiliation ou le contexte admin tenant).
     */
    @PostMapping(path = "switch-school")
    public Map<String, String> switchSchool(
        @RequestBody SwitchSchoolRequest request,
        @AuthenticationPrincipal User principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (request == null || request.schoolId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId requis.");
        }
        return this.jwtService.switchActiveSchool(principal.getEmail(), request.schoolId());
    }

    @PostMapping(path = "login")
    public Map<String, String> login(@RequestBody LoginRequest request) {
        try {
            final Authentication authentication = this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.userName(), request.password())
            );
            if (authentication.isAuthenticated()) {
                return this.jwtService.generate(request.userName(), true);
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion refusée.");
        } catch (UsernameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Aucun compte n'est associé à cette adresse e-mail.");
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect.");
        } catch (DisabledException e) {
            // Compte inactif : Spring vérifie souvent l’état avant le mot de passe → ne pas utiliser 403
            // (confusion avec « droits ») ; message explicite pour le front.
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Ce compte n'est pas encore activé. Utilisez le lien reçu par e-mail ou la page d'activation."
            );
        } catch (LockedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte est verrouillé. Contactez l'administration.");
        } catch (AuthenticationException e) {
            log.debug("Échec d'authentification : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect.");
        }
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

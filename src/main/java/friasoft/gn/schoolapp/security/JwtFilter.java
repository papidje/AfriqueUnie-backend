package friasoft.gn.schoolapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import friasoft.gn.schoolapp.entity.auth.Jwt;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.auth.UserPlatformRole;
import friasoft.gn.schoolapp.repository.UserPlatformRoleRepository;
import friasoft.gn.schoolapp.service.UserService;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EntityNotFoundException;

@Service
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final UserPlatformRoleRepository userPlatformRoleRepository;

    public JwtFilter(
        UserService userService,
        JwtService jwtService,
        ObjectMapper objectMapper,
        UserPlatformRoleRepository userPlatformRoleRepository
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.userPlatformRoleRepository = userPlatformRoleRepository;
    }

    private static boolean shouldParseBearerForAuthPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (!path.startsWith("/auth/")) {
            return true;
        }
        return "/auth/logout".equals(path) || "/auth/switch-school".equals(path);
    }

    /**
     * Chemin sans context-path (Spring {@code server.servlet.context-path}, ex. {@code /api/rest}).
     */
    private static String normalizedPathWithinContext(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        int q = uri.indexOf('?');
        if (q >= 0) {
            uri = uri.substring(0, q);
        }
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri.isEmpty() ? "/" : uri;
    }

    /**
     * Liste et compteur non lu du centre de notifications : accessibles avec JWT valide même sans tenant courant
     * dans le jeton (utilisateur sans affiliation active / sans école résolvable pour la session).
     */
    private static boolean isSchoollessNotificationsRead(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String p = normalizedPathWithinContext(request);
        return "/notifications".equals(p) || "/notifications/unread-count".equals(p);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        Jwt savedJwt = null;
        String username = null;
        Long tenantId = null;
        boolean isTokenExpired = true;
        /** Utilisateur fraîchement chargé pour la session Bearer (évite un double accès base). */
        User bearerResolvedUser = null;

        try {
            if (!shouldParseBearerForAuthPath(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer")){
                token = authorization.substring(7);
                try {
                    savedJwt = this.jwtService.tokenByValue(token);
                } catch (EntityNotFoundException ex) {
                    ProblemDetailHttpResponses.writeUnauthorized(
                        response,
                        objectMapper,
                        "Jeton d’accès inconnu ou révoqué."
                    );
                    return;
                }
                isTokenExpired = jwtService.isTokenExpired(token);
                if (isTokenExpired) {
                    ProblemDetailHttpResponses.writeUnauthorized(
                        response,
                        objectMapper,
                        "Jeton d’accès expiré. La session va être rafraîchie automatiquement."
                    );
                    return;
                }
                username = jwtService.extractUserName(token);
                tenantId = jwtService.extractTenantId(token);
                User jwtUser = savedJwt.getUser();
                if (tenantId == null && jwtUser.getSchool() != null) {
                    tenantId = jwtUser.getSchool().getTenantId();
                }
                UserPlatformRole platform = userPlatformRoleRepository.findByUser_Id(jwtUser.getId()).orElse(null);
                if (tenantId == null && platform != null && platform.getRole() == User.UserRole.ADMIN_ECOLE) {
                    if (jwtUser.getOrganizationTenantId() != null) {
                        tenantId = jwtUser.getOrganizationTenantId();
                    } else if (jwtUser.getTenantId() != null) {
                        tenantId = jwtUser.getTenantId();
                    }
                }
                if (tenantId == null && jwtUser.getTenantId() != null) {
                    tenantId = jwtUser.getTenantId();
                }
                boolean isSuperAdmin = platform != null && platform.getRole() == User.UserRole.SUPER_ADMIN;
                boolean schoollessNotificationsRead = isSchoollessNotificationsRead(request);
                if (!isSuperAdmin && tenantId == null && !schoollessNotificationsRead) {
                    ProblemDetailHttpResponses.writeForbidden(
                        response,
                        objectMapper,
                        "Tenant introuvable pour cette session (JWT ou profil). Reconnectez-vous ou contactez le support."
                    );
                    return;
                }
                /*
                 * Tenant Hibernate : pour les GET notifications sans tenant JWT, charger l’utilisateur d’abord
                 * sans filtre tenant (pas de {@code TenantContext} encore), puis réinjecter le tenant issu du profil si présent.
                 */
                if (!(schoollessNotificationsRead && tenantId == null)) {
                    TenantContext.setTenantId(tenantId);
                }
                bearerResolvedUser = userService.loadUserByUsername(username);
                if (schoollessNotificationsRead && tenantId == null && !isSuperAdmin) {
                    Long resolvedTenant = bearerResolvedUser.getTenantId();
                    if (resolvedTenant == null) {
                        resolvedTenant = bearerResolvedUser.getOrganizationTenantId();
                    }
                    if (resolvedTenant != null) {
                        TenantContext.setTenantId(resolvedTenant);
                    }
                }
                if (!bearerResolvedUser.isActive()) {
                    ProblemDetailHttpResponses.writeForbiddenAccountDisabled(
                        response,
                        objectMapper,
                        "Ce compte a été désactivé. Vous ne pouvez plus utiliser SchoolApp avec cette session."
                    );
                    return;
                }
            }

            if (!isTokenExpired
                    && savedJwt != null
                    && token != null
                    && savedJwt.getUser().getEmail().equals(username)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = bearerResolvedUser != null ? bearerResolvedUser : userService.loadUserByUsername(username);
                List<GrantedAuthority> authorities = jwtService.extractAuthorities(token);
                if (authorities.isEmpty()) {
                    authorities = new ArrayList<>(jwtService.fallbackAuthoritiesFromUserAndToken(user, token));
                }
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                authenticationToken.getAuthorities().forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

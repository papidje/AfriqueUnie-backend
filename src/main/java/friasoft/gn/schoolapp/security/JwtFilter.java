package friasoft.gn.schoolapp.security;

import friasoft.gn.schoolapp.entity.auth.Jwt;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.service.UserService;
import friasoft.gn.schoolapp.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Service
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private UserService userService;
    private JwtService jwtService;

    public JwtFilter(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    private static boolean shouldParseBearerForAuthPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (!path.startsWith("/auth/")) {
            return true;
        }
        return "/auth/logout".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token;
        Jwt savedJwt = null;
        String username = null;
        Long tenantId = null;
        boolean isTokenExpired = true;

        try {
            if (!shouldParseBearerForAuthPath(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer")){
                token = authorization.substring(7);
                savedJwt = this.jwtService.tokenByValue(token);
                isTokenExpired = jwtService.isTokenExpired(token);
                username = jwtService.extractUserName(token);
                tenantId = jwtService.extractTenantId(token);
                if (tenantId == null && savedJwt.getUser().getSchool() != null) {
                    tenantId = savedJwt.getUser().getSchool().getId();
                }
                boolean isSuperAdmin = savedJwt.getUser().getRole() == User.UserRole.SUPER_ADMIN;
                if (!isSuperAdmin && tenantId == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant manquant dans le JWT");
                    return;
                }
                TenantContext.setTenantId(tenantId);
                userService.loadUserByUsername(username);
            }

            if (!isTokenExpired
                    && savedJwt != null
                    && savedJwt.getUser().getEmail().equals(username)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                authenticationToken.getAuthorities().forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

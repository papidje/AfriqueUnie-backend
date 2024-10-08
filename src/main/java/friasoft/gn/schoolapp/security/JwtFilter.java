package friasoft.gn.schoolapp.security;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import friasoft.gn.schoolapp.entity.User;
import friasoft.gn.schoolapp.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class JwtFilter extends OncePerRequestFilter {

    private UserService userService;
    private JwtService jwtService;

    public JwtFilter(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, java.io.IOException {
        String token = null;
        String username = null;
        boolean isTokenExpired = true;

        String authorization = request.getHeader("Authorization");
        if(authorization != null && authorization.startsWith("Bearer")){
            token = authorization.substring(7);
            isTokenExpired = jwtService.isTokenExpired(token);
            username = jwtService.extractUserName(token);
            userService.loadUserByUsername(username);
        }

        if(!isTokenExpired && username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = userService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        filterChain.doFilter(request, response);
    }
}

package friasoft.gn.schoolapp.security;

import friasoft.gn.schoolapp.entity.auth.Jwt;
import friasoft.gn.schoolapp.entity.auth.RefreshToken;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.IActivationRepository;
import friasoft.gn.schoolapp.repository.IJwtRepository;
import friasoft.gn.schoolapp.repository.TenantRepository;
import friasoft.gn.schoolapp.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Transactional
@Service
@AllArgsConstructor
@Slf4j
public class JwtService {
    private static final String BEARER = "bearer";
    public static final String REFRESH = "refresh";
    private UserService userService;
    private IJwtRepository iJwtRepository;
    private IActivationRepository iActivationRepository;
    private TenantRepository tenantRepository;
//    @Value("${application.jwt.encriptionKey}")
    private static String ENCRYPTION_KEY = "b58907b9ad3493eaa357bf390e6739209cb5cfc43c3fc9647d7e0d94a0ed98dd";
//    @Value("${application.jwt.duration}")
    private static long tokenDurarion = 30;
    
    public Map<String, String> generate(String username, boolean fromLogin) {
        User user = userService.loadUserByUsername(username);
        if (fromLogin) {
            user.setLastLoginAt(Instant.now());
            userService.saveUser(user);
        }
        this.disableTokens(user);
        final Map<String, String> jwtMap = new java.util.HashMap<>(this.generateJwt(user));

        RefreshToken refreshToken = RefreshToken.builder()
                .value(UUID.randomUUID().toString())
                .expired(false)
                .creation(Instant.now())
                .expiration(Instant.now().plusSeconds(3 * 24 * 60 * 60))
                .build();

        Jwt jwt = Jwt
            .builder()
            .isActive(true)
            .isExpired(false)
            .user(user)
            .refreshToken(refreshToken)
            .jwt(jwtMap.get(BEARER))
            .lastLoginAt(user.getLastLoginAt())
            .build();
        this.iJwtRepository.save(jwt);
        jwtMap.put("refresh", refreshToken.getValue());
        return jwtMap;
    }

    public String extractUserName(String token) {
        return this.getClain(token, Claims::getSubject);
    }

    public Long extractTenantId(String token) {
        return this.getClain(token, claims -> claims.get("tenant_id", Long.class));
    }
    
    public boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate.before(new Date());
    }

    private Map<String, String> generateJwt(User user) {
        final long currentTime = System.currentTimeMillis();
        final long expirationTime = currentTime + 30 * 60 * 1000;
        final Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("name", user.getFullname());
        claims.put("email", user.getEmail());
        claims.put("username", user.getUsername());
        Long tenantClaim = user.getOrganizationTenantId();
        if (tenantClaim == null) {
            tenantClaim = user.getTenantId();
        }
        claims.put("tenant_id", tenantClaim);
        claims.put(Claims.EXPIRATION, new Date(expirationTime));
        claims.put(Claims.SUBJECT, user.getEmail());
        claims.put("roles", user.getAuthorities());
        claims.put("lastLoginAt", Date.from(user.getLastLoginAt()));
        // Pas de school_id pour ADMIN_ECOLE : le compte couvre tout le tenant, l’établissement actif est choisi en session (UI).
        Long schoolScopeId = null;
        if (user.getRole() != User.UserRole.ADMIN_ECOLE
            && user.getSchool() != null
            && user.getSchool().getId() != null) {
            schoolScopeId = user.getSchool().getId();
        }
        if (schoolScopeId != null) {
            claims.put("school_id", schoolScopeId);
        }
        if (user.getRole() != User.UserRole.SUPER_ADMIN) {
            String headerTitle = resolveHeaderTitle(user);
            if (headerTitle != null && !headerTitle.isBlank()) {
                claims.put("header_title", headerTitle);
            }
        }
        final String bearer = Jwts.builder()
            .issuedAt(new Date(currentTime))
            .expiration(new Date(expirationTime))
            .subject(user.getEmail())
            .claims(claims)
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
        return Map.of(BEARER, bearer);
    }

    private String resolveHeaderTitle(User user) {
        if (user.getRole() == User.UserRole.ADMIN_ECOLE) {
            Long tenantId = user.getOrganizationTenantId();
            if (tenantId == null) {
                return null;
            }
            return tenantRepository.findById(tenantId)
                .map(t -> t.getName())
                .filter(n -> n != null && !n.isBlank())
                .orElse(null);
        }
        if (user.getRole() == User.UserRole.DIRECTOR
            || user.getRole() == User.UserRole.STAFF
            || user.getRole() == User.UserRole.TEACHER
            || user.getRole() == User.UserRole.ACCOUNTANT) {
            School school = user.getSchool();
            if (school == null) {
                return null;
            }
            String name = school.getName();
            if (name == null || name.isBlank()) {
                return null;
            }
            return name;
        }
        return null;
    }

    public Jwt tokenByValue(String token) {
        return this.iJwtRepository.findByJwt(token).orElseThrow(() -> new EntityNotFoundException());
    }

    private Key getKey() {
        final byte[] decoder = Decoders.BASE64.decode(ENCRYPTION_KEY);
        return Keys.hmacShaKeyFor(decoder);
    }

    private Date getExpirationDateFromToken(String token) {
        return this.getClain(token, Claims::getExpiration);
//        return new Date();
    }

    private <T> T getClain(String token, Function<Claims, T> function) {
        Claims claims = getAllClaims(token);
        return function.apply(claims);
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser()
            .setSigningKey(this.getKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public void logout() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Jwt jwt = this.iJwtRepository
                .findUserValidToken(user.getEmail(), true, false)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        jwt.setExpired(true);
        jwt.setActive(false);
        this.iJwtRepository.save(jwt);
    }

    @Scheduled(cron = "0 0 */1 * * * ")
    public void removeUselessToken() {
        log.info("remove useless token at {}", Instant.now());
        this.iJwtRepository.deleteAllByIsActiveAndIsExpired(false, true);
    }

    @Scheduled(cron = "0 0 */1 * * * ")
    public void removeUselessActivation() {
        log.info("remove useless activation at {}", Instant.now());
        this.iActivationRepository.deleteAllByExpirationBefore(Instant.now());
    }

    private void disableTokens(User user) {
        List<Jwt> jwts = this.iJwtRepository.findByEmail(user.getEmail()).peek(
            jwt -> {
                jwt.setActive(false);
                jwt.setExpired(true);
            }
        ).toList();
        this.iJwtRepository.saveAll(jwts);
    }

    public Map<String, String> refreshToken(Map<String, String> refreshTokenRequest) {
        Jwt jwt = this.iJwtRepository.findByRefreshToken(refreshTokenRequest.get(REFRESH))
                .orElseThrow(() -> new RuntimeException("Token not found"));
        if (jwt.getRefreshToken().isExpired() || jwt.getRefreshToken().getExpiration().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token expired");
        }
        this.disableTokens(jwt.getUser());
        return this.generate(jwt.getUser().getEmail(), false);
    }
}

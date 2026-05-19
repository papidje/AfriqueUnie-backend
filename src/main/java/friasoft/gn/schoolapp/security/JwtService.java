package friasoft.gn.schoolapp.security;

import friasoft.gn.schoolapp.exception.AccountDisabledException;
import friasoft.gn.schoolapp.entity.auth.Jwt;
import friasoft.gn.schoolapp.entity.auth.RefreshToken;
import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.repository.IActivationRepository;
import friasoft.gn.schoolapp.repository.IJwtRepository;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.TenantRepository;
import friasoft.gn.schoolapp.entity.auth.UserPlatformRole;
import friasoft.gn.schoolapp.repository.UserPlatformRoleRepository;
import friasoft.gn.schoolapp.repository.UserSchoolAffiliationRepository;
import friasoft.gn.schoolapp.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Key;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Transactional
@Service
@AllArgsConstructor
@Slf4j
public class JwtService {
    private static final String BEARER = "bearer";
    public static final String REFRESH = "refresh";
    private final UserService userService;
    private final IJwtRepository iJwtRepository;
    private final IActivationRepository iActivationRepository;
    private final TenantRepository tenantRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolSecurity schoolSecurity;
    private final UserSchoolAffiliationRepository userSchoolAffiliationRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;

    private static String ENCRYPTION_KEY = "b58907b9ad3493eaa357bf390e6739209cb5cfc43c3fc9647d7e0d94a0ed98dd";

    private record SessionContext(Long activeSchoolId, List<GrantedAuthority> jwtAuthorities) {}

    public Map<String, String> generate(String username, boolean fromLogin) {
        User user = userService.loadUserByUsername(username);
        if (!user.isActive()) {
            throw new AccountDisabledException();
        }
        if (fromLogin) {
            user.setLastLoginAt(Instant.now());
            userService.saveUser(user);
        }
        disableTokens(user);
        SessionContext ctx = resolveSessionContextForLogin(user);
        return issueFullTokenPair(user, ctx.activeSchoolId(), ctx.jwtAuthorities(), user.getLastLoginAt());
    }

    /**
     * Régénère uniquement la chaîne JWT d’accès pour une école donnée (claims {@code school_id} + {@code roles}
     * = rôles effectifs pour cet établissement). Ne persiste pas de refresh — utiliser {@link #switchActiveSchool}.
     */
    public Map<String, String> generateJwtWithCustomSchool(User user, Long schoolId) {
        if (schoolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId requis.");
        }
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "École introuvable."));
        assertSchoolAffiliationActiveForSwitch(user, school);
        if (!schoolSecurity.canAccessSchool(user, school)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Établissement non accessible pour ce compte.");
        }
        List<GrantedAuthority> authorities = resolveAuthoritiesForSchool(user, schoolId);
        String bearer = buildJwtCompact(user, school, schoolId, authorities);
        return Map.of(BEARER, bearer);
    }

    /**
     * Nouvelle session complète (access + refresh) pour l’établissement choisi, après révocation des jetons existants.
     */
    public Map<String, String> switchActiveSchool(String username, Long schoolId) {
        if (schoolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId requis.");
        }
        User user = userService.loadUserByUsername(username);
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "École introuvable."));
        assertSchoolAffiliationActiveForSwitch(user, school);
        if (!schoolSecurity.canAccessSchool(user, school)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Établissement non accessible pour ce compte.");
        }
        List<GrantedAuthority> authorities = resolveAuthoritiesForSchool(user, schoolId);
        disableTokens(user);
        return issueFullTokenPair(user, schoolId, authorities, user.getLastLoginAt());
    }

    /**
     * Refuse le basculement vers une école où l’utilisateur a des lignes d’affiliation mais aucune active
     * (suspension ou invitation non acceptée). Les admins d’organisation sans lignes pour cette école ne sont pas concernés.
     */
    private void assertSchoolAffiliationActiveForSwitch(User user, School school) {
        long total = userSchoolAffiliationRepository.countByUserIdAndSchoolId(user.getId(), school.getId());
        if (total == 0) {
            return;
        }
        long active = userSchoolAffiliationRepository.countActiveByUserIdAndSchoolId(user.getId(), school.getId());
        if (active == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès suspendu pour cet établissement.");
        }
    }

    public String extractUserName(String token) {
        return this.getClain(token, Claims::getSubject);
    }

    public Long extractTenantId(String token) {
        return this.getClain(token, claims -> {
            Object raw = claims.get("tenant_id");
            if (raw == null) {
                return null;
            }
            if (raw instanceof Long l) {
                return l;
            }
            if (raw instanceof Integer i) {
                return i.longValue();
            }
            if (raw instanceof Number n) {
                return n.longValue();
            }
            return null;
        });
    }

    /**
     * Autorités Spring alignées sur le claim {@code roles} du JWT (tous les rôles effectifs pour l’école active).
     */
    public List<GrantedAuthority> extractAuthorities(String token) {
        try {
            Claims claims = getAllClaims(token);
            List<GrantedAuthority> parsed = parseRolesClaim(claims.get("roles"));
            if (!parsed.isEmpty()) {
                return parsed;
            }
        } catch (RuntimeException ex) {
            log.debug("Impossible d’extraire les rôles du JWT : {}", ex.getMessage());
        }
        return List.of();
    }

    private static List<GrantedAuthority> parseRolesClaim(Object rolesObj) {
        List<GrantedAuthority> out = new ArrayList<>();
        if (!(rolesObj instanceof Collection<?> col)) {
            return out;
        }
        for (Object o : col) {
            if (o instanceof GrantedAuthority ga) {
                out.add(ga);
            } else if (o instanceof Map<?, ?> m) {
                Object auth = m.get("authority");
                if (auth instanceof String s && !s.isBlank()) {
                    out.add(new SimpleGrantedAuthority(s));
                }
            } else if (o instanceof String s && !s.isBlank()) {
                out.add(new SimpleGrantedAuthority(s));
            }
        }
        return out;
    }

    public boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate.before(new Date());
    }

    private SessionContext resolveSessionContextForLogin(User user) {
        Optional<UserPlatformRole> plat = userPlatformRoleRepository.findByUser_Id(user.getId());
        User.UserRole platform = plat.map(UserPlatformRole::getRole).orElse(null);
        if (platform == User.UserRole.ADMIN_ECOLE) {
            return new SessionContext(null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN_ECOLE")));
        }
        if (platform == User.UserRole.SUPER_ADMIN) {
            return new SessionContext(null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        }
        if (user.getSchool() != null && user.getSchool().getId() != null) {
            Long sid = user.getSchool().getId();
            return new SessionContext(sid, resolveAuthoritiesForSchool(user, sid));
        }
        List<School> schools = userSchoolAffiliationRepository.findActiveSchoolsForUser(user.getId());
        if (!schools.isEmpty()) {
            Long sid = schools.get(0).getId();
            return new SessionContext(sid, resolveAuthoritiesForSchool(user, sid));
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aucun contexte école pour ce compte.");
    }

    /**
     * Rôles pour une école donnée : plateforme ({@code ADMIN_ECOLE} / {@code SUPER_ADMIN}) ou toutes les lignes
     * d’affiliation actives ({@code DIRECTOR}, {@code STAFF}, {@code TEACHER}, …).
     */
    private List<GrantedAuthority> resolveAuthoritiesForSchool(User user, Long schoolId) {
        Optional<UserPlatformRole> plat = userPlatformRoleRepository.findByUser_Id(user.getId());
        User.UserRole platform = plat.map(UserPlatformRole::getRole).orElse(null);
        if (platform == User.UserRole.SUPER_ADMIN) {
            return List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        if (platform == User.UserRole.ADMIN_ECOLE) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN_ECOLE"));
        }
        List<String> roleNames =
            userSchoolAffiliationRepository.findActiveRoleNamesForUserAndSchool(user.getId(), schoolId);
        if (!roleNames.isEmpty()) {
            LinkedHashSet<GrantedAuthority> set = new LinkedHashSet<>();
            for (String roleName : roleNames) {
                if (roleName != null && !roleName.isBlank()) {
                    set.add(new SimpleGrantedAuthority("ROLE_" + roleName.trim()));
                }
            }
            if (!set.isEmpty()) {
                return List.copyOf(set);
            }
        }
        if (user.getSchool() != null
            && user.getSchool().getId() != null
            && user.getSchool().getId().equals(schoolId)) {
            return List.of(new SimpleGrantedAuthority("ROLE_STAFF"));
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aucun rôle actif pour cet établissement.");
    }

    private Map<String, String> issueFullTokenPair(
        User user,
        Long activeSchoolId,
        List<GrantedAuthority> jwtAuthorities,
        Instant lastLoginAt
    ) {
        School schoolEntity = activeSchoolId != null
            ? schoolRepository.findById(activeSchoolId).orElse(null)
            : null;
        if (activeSchoolId != null && schoolEntity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "École introuvable.");
        }
        String bearer = buildJwtCompact(user, schoolEntity, activeSchoolId, jwtAuthorities);

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
            .jwt(bearer)
            .lastLoginAt(lastLoginAt)
            .activeSchoolId(activeSchoolId)
            .build();
        this.iJwtRepository.save(jwt);
        Map<String, String> jwtMap = new java.util.HashMap<>();
        jwtMap.put(BEARER, bearer);
        jwtMap.put("refresh", refreshToken.getValue());
        return jwtMap;
    }

    private String buildJwtCompact(
        User user,
        School schoolOrNull,
        Long schoolIdForClaim,
        List<GrantedAuthority> jwtAuthorities
    ) {
        final long currentTime = System.currentTimeMillis();
        final long expirationTime = currentTime + 30 * 60 * 1000;
        final Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("name", user.getFullname());
        claims.put("email", user.getEmail());
        claims.put("username", user.getUsername());

        Long tenantClaim = null;
        Optional<UserPlatformRole> platForTenant = userPlatformRoleRepository.findByUser_Id(user.getId());
        User.UserRole platformRole = platForTenant.map(UserPlatformRole::getRole).orElse(null);
        boolean tenantWidePlatform =
            platformRole == User.UserRole.SUPER_ADMIN || platformRole == User.UserRole.ADMIN_ECOLE;
        // Contexte école explicite : le tenant JWT doit suivre l’établissement actif (invitations cross-tenant).
        if (!tenantWidePlatform && schoolOrNull != null && schoolOrNull.getTenantId() != null) {
            tenantClaim = schoolOrNull.getTenantId();
        }
        if (tenantClaim == null) {
            tenantClaim = user.getOrganizationTenantId();
        }
        if (tenantClaim == null) {
            tenantClaim = user.getTenantId();
        }
        if (tenantClaim == null && schoolOrNull != null) {
            tenantClaim = schoolOrNull.getTenantId();
        }
        claims.put("tenant_id", tenantClaim);

        claims.put(Claims.EXPIRATION, new Date(expirationTime));
        claims.put(Claims.SUBJECT, user.getEmail());
        claims.put("roles", new ArrayList<>(jwtAuthorities));
        claims.put("lastLoginAt", Date.from(user.getLastLoginAt()));

        if (schoolIdForClaim != null) {
            claims.put("school_id", schoolIdForClaim);
        }

        if (!hasAuthority(jwtAuthorities, "ROLE_SUPER_ADMIN")) {
            String headerTitle = resolveHeaderTitle(user, jwtAuthorities, schoolOrNull);
            if (headerTitle != null && !headerTitle.isBlank()) {
                claims.put("header_title", headerTitle);
            }
        }

        if (schoolOrNull != null) {
            if (schoolOrNull.getThemeName() != null && !schoolOrNull.getThemeName().isBlank()) {
                claims.put("school_theme", schoolOrNull.getThemeName().trim());
            }
            if (schoolOrNull.getFontName() != null && !schoolOrNull.getFontName().isBlank()) {
                claims.put("school_font", schoolOrNull.getFontName().trim());
            }
        }

        return Jwts.builder()
            .issuedAt(new Date(currentTime))
            .expiration(new Date(expirationTime))
            .subject(user.getEmail())
            .claims(claims)
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    private static boolean hasAuthority(List<GrantedAuthority> jwtAuthorities, String authority) {
        for (GrantedAuthority ga : jwtAuthorities) {
            if (authority.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private String resolveHeaderTitle(User user, List<GrantedAuthority> jwtAuthorities, School schoolOrNull) {
        if (schoolOrNull != null && schoolOrNull.getName() != null && !schoolOrNull.getName().isBlank()) {
            return schoolOrNull.getName().trim();
        }
        if (hasAuthority(jwtAuthorities, "ROLE_ADMIN_ECOLE")) {
            Long tenantId = user.getOrganizationTenantId();
            if (tenantId == null) {
                tenantId = user.getTenantId();
            }
            if (tenantId == null) {
                return null;
            }
            return tenantRepository.findById(tenantId)
                .map(t -> t.getName())
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .orElse(null);
        }
        return null;
    }

    public Jwt tokenByValue(String token) {
        return this.iJwtRepository.findFirstByJwtOrderByIdDesc(token).orElseThrow(() -> new EntityNotFoundException());
    }

    private Key getKey() {
        final byte[] decoder = Decoders.BASE64.decode(ENCRYPTION_KEY);
        return Keys.hmacShaKeyFor(decoder);
    }

    private Date getExpirationDateFromToken(String token) {
        return this.getClain(token, Claims::getExpiration);
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
        String refreshValue = refreshTokenRequest != null ? refreshTokenRequest.get(REFRESH) : null;
        if (refreshValue == null || refreshValue.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token required");
        }
        Jwt jwt = this.iJwtRepository.findByRefreshToken(refreshValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        RefreshToken rt = jwt.getRefreshToken();
        if (rt == null || rt.isExpired() || rt.getExpiration().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        User user = jwt.getUser();
        if (!user.isActive()) {
            throw new AccountDisabledException();
        }
        Long activeSchoolId = jwt.getActiveSchoolId();
        this.disableTokens(user);

        try {
            if (activeSchoolId != null) {
                School school = schoolRepository.findById(activeSchoolId).orElse(null);
                if (school != null && schoolSecurity.canAccessSchool(user, school)) {
                    List<GrantedAuthority> authorities = resolveAuthoritiesForSchool(user, activeSchoolId);
                    return issueFullTokenPair(user, activeSchoolId, authorities, user.getLastLoginAt());
                }
            }
        } catch (ResponseStatusException ex) {
            log.debug("Refresh : contexte école {} invalide, repli login : {}", activeSchoolId, ex.getReason());
        }

        SessionContext ctx = resolveSessionContextForLogin(userService.loadUserByUsername(user.getEmail()));
        return issueFullTokenPair(user, ctx.activeSchoolId(), ctx.jwtAuthorities(), user.getLastLoginAt());
    }

    /**
     * Repli si le claim {@code roles} n’a pas pu être relu (anciens jetons) : recalcule depuis {@code school_id} ou le contexte login.
     */
    public List<GrantedAuthority> fallbackAuthoritiesFromUserAndToken(User user, String token) {
        try {
            Claims claims = getAllClaims(token);
            Object sidRaw = claims.get("school_id");
            Long schoolId = null;
            if (sidRaw instanceof Number n) {
                schoolId = n.longValue();
            }
            if (schoolId != null) {
                return resolveAuthoritiesForSchool(user, schoolId);
            }
            return resolveSessionContextForLogin(user).jwtAuthorities();
        } catch (RuntimeException ex) {
            log.debug("fallbackAuthoritiesFromUserAndToken : {}", ex.getMessage());
            return List.of();
        }
    }
}

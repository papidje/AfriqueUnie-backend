package friasoft.gn.schoolapp.security;

import friasoft.gn.schoolapp.entity.Jwt;
import friasoft.gn.schoolapp.entity.User;
import friasoft.gn.schoolapp.repository.IJwtRepository;
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
import java.util.function.Function;

@Transactional
@Service
@AllArgsConstructor
@Slf4j
public class JwtService {
    private static final String BEARER = "bearer";
    private UserService userService;
    private IJwtRepository iJwtRepository;
//    @Value("${application.jwt.encriptionKey}")
    private static String ENCRYPTION_KEY = "b58907b9ad3493eaa357bf390e6739209cb5cfc43c3fc9647d7e0d94a0ed98dd";
//    @Value("${application.jwt.duration}")
    private static long tokenDurarion = 30;
    
    public Map<String, String> generate(String username) {
        User user = userService.loadUserByUsername(username);
        this.disableTokens(user);
        Map<String, String> jwtMap = this.generateJwt(user);

        Jwt jwt = Jwt
            .builder()
            .isActive(true)
            .isExpired(false)
            .user(user)
            .jwt(jwtMap.get(BEARER))
            .build();
        this.iJwtRepository.save(jwt);
        return jwtMap;
    }

    public String extractUserName(String token) {
        return this.getClain(token, Claims::getSubject);
    }
    
    public boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate.before(new Date());
    }

    private Map<String, String> generateJwt(User user) {
        final long currentTime = System.currentTimeMillis();
        final long expirationTime = currentTime + tokenDurarion * 60 * 1000;
        final Map<String, Object> claims = Map.of(
            "name", user.getName(),
            "email", user.getEmail(),
            "username", user.getUsername(),
            Claims.EXPIRATION, new Date(expirationTime),
            Claims.SUBJECT, user.getEmail(),
            "roles", user.getAuthorities()
        );
        final String bearer = Jwts.builder()
            .setIssuedAt(new Date(currentTime))
            .setExpiration(new Date(expirationTime))
            .setSubject(user.getEmail())
            .setClaims(claims)
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
        return Map.of(BEARER, bearer);
    }

    public Jwt tokenByvalue(String token) {
        return this.iJwtRepository.findByJwt(token).orElseThrow(() -> new EntityNotFoundException());
    }

    private Key getKey() {
        final byte[] decoder = Decoders.BASE64.decode(ENCRYPTION_KEY);
        return Keys.hmacShaKeyFor(decoder);
    }

    private Date getExpirationDateFromToken(String token) {
        this.getClain(token, Claims::getExpiration);
        return new Date();
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

    @Scheduled(cron = "0 */1 * * * * ")
    public void removeUselessToken() {
        log.info("remove useless token at {}", Instant.now());
        this.iJwtRepository.deleteAllByIsActiveAndIsExpired(false, true);
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
}

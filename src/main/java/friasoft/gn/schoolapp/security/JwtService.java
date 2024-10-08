package friasoft.gn.schoolapp.security;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import friasoft.gn.schoolapp.entity.User;
import friasoft.gn.schoolapp.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class JwtService {
    private final String ENCRYPTION_KEY="b58907b9ad3493eaa357bf390e6739209cb5cfc43c3fc9647d7e0d94a0ed98dd";
    private UserService userService;
    
    public Map<String, String> generate(String username) {
        User user = userService.loadUserByUsername(username);
        return this.generateJwt(user);
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
        final long expirationTime = currentTime + 30 * 60 * 1000;
        final Map<String, Object> claims = Map.of(
            "name", user.getName(),
            "email", user.getEmail(),
            Claims.EXPIRATION, new Date(expirationTime),
            Claims.SUBJECT, user.getEmail()
        );
        final String bearer = Jwts.builder()
            .setIssuedAt(new Date(currentTime))
            .setExpiration(new Date(expirationTime))
            .setSubject(user.getEmail())
            .setClaims(claims)
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
        return Map.of("bearer", bearer);
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
}

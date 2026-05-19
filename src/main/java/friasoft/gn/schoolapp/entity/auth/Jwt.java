package friasoft.gn.schoolapp.entity.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jwts")
public class Jwt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 4096)
    private String jwt;

    private boolean isActive;

    private boolean isExpired;

    private Instant createdAt = Instant.now();

    private Instant lastLoginAt = Instant.now();

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private RefreshToken refreshToken;

    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE})
    private User user;

    /** Établissement porté par les claims du dernier access token (refresh conserve le contexte). */
    @Column(name = "active_school_id")
    private Long activeSchoolId;
}

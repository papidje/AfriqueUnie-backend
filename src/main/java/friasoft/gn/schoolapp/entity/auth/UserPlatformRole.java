package friasoft.gn.schoolapp.entity.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rôle « hors établissement » : super-admin ou administrateur d’organisation ({@link User.UserRole}).
 * Les rôles métier par école vivent dans {@link UserSchoolAffiliation}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_platform_roles")
public class UserPlatformRole {

    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private User.UserRole role;
}

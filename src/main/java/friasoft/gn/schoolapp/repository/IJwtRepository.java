package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.auth.Jwt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface IJwtRepository extends JpaRepository<Jwt, Integer>{

    @Query("FROM Jwt j WHERE j.isExpired = :isExpired AND j.isActive = :isActive AND j.user.email = :email")
    Optional<Jwt> findUserValidToken(String email, boolean isActive, boolean isExpired);

    @Query("FROM Jwt j WHERE j.user.email = :email")
    Stream<Jwt> findByEmail(String email);

    @Query("FROM Jwt j WHERE j.refreshToken.value = :value")
    Optional<Jwt> findByRefreshToken(String value);

    Optional<Jwt> findByJwt(String jwt);

    void deleteAllByIsActiveAndIsExpired(boolean isActive, boolean isExpired);
}

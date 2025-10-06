package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.auth.Activation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface IActivationRepository extends JpaRepository<Activation, Short> {

//    List<Activation> findAllBySchoolId(short schoolId);
    Optional<Activation> findByCode(String code);

    void deleteAllByExpirationBefore(Instant now);
}

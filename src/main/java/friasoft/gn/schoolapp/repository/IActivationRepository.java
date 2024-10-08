package friasoft.gn.schoolapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import friasoft.gn.schoolapp.entity.Activation;

public interface IActivationRepository extends JpaRepository<Activation, Short> {

    List<Activation> findAllBySchoolId(short schoolId);
    Optional<Activation> findByCode(String code);
}

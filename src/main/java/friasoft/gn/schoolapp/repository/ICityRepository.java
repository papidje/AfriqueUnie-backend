package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICityRepository extends JpaRepository<City, Long> {

    List<City> findByActiveTrueOrderByNameAsc();

    List<City> findAllByOrderByNameAsc();

    Optional<City> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}

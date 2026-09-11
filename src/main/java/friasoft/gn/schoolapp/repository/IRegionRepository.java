package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IRegionRepository extends JpaRepository<Region, Long> {

    List<Region> findByActiveTrueOrderByNameAsc();

    List<Region> findAllByOrderByNameAsc();

    Optional<Region> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}

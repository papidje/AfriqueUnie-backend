package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// ClassLevelGroupRepository.java
@Repository
public interface IClassLevelGroupRepository extends JpaRepository<ClassLevelGroup, Long> {
    Optional<ClassLevelGroup> findByCode(String code);
}

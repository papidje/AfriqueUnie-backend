package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.ClassLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IClassLevelRepository extends JpaRepository<ClassLevel, Long> {
    List<ClassLevel> findByGroup_Code(String groupCode);

    Optional<ClassLevel> findByCode(String code);

    @Query("""
        select distinct lv from ClassLevel lv
        left join fetch lv.group
        """)
    List<ClassLevel> findAllWithGroup();
}

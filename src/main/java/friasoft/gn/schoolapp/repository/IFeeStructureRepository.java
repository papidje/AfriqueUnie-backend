package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IFeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    @Query("""
        select fs
        from FeeStructure fs
        join fetch fs.classLevel cl
        join fetch fs.schoolYear sy
        where fs.id = :id
        """)
    Optional<FeeStructure> findByIdWithRefs(@Param("id") Long id);

    @Query("""
        select fs
        from FeeStructure fs
        join fetch fs.classLevel cl
        left join fetch cl.group
        join fetch fs.schoolYear sy
        where sy.id = :schoolYearId
        """)
    List<FeeStructure> findAllBySchoolYearIdWithRefs(@Param("schoolYearId") Long schoolYearId);

    boolean existsByClassLevel_IdAndSchoolYear_Id(Long classLevelId, Long schoolYearId);

    Optional<FeeStructure> findByClassLevel_IdAndSchoolYear_Id(Long classLevelId, Long schoolYearId);
}

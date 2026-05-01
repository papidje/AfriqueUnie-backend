package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// SchoolClassRepository.java
@Repository
public interface ISchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    List<SchoolClass> findByYear_Id(Long yearId);

    @Query("""
        select distinct sc from SchoolClass sc
        join fetch sc.level lv
        join fetch lv.group
        join fetch sc.year y
        where y.school.id = :schoolId and y.active = true
        """)
    List<SchoolClass> findByYear_School_IdAndYear_ActiveTrue(@Param("schoolId") Long schoolId);

    @Query("""
        select count(sc)
        from SchoolClass sc
        join sc.year y
        where y.school.id = :schoolId and y.active = true
        """)
    long countActiveSchoolYearClasses(@Param("schoolId") Long schoolId);

    @Query("""
        select coalesce(sum(sc.capacity), 0)
        from SchoolClass sc
        join sc.year y
        where y.school.id = :schoolId and y.active = true
        """)
    long sumCapacityActiveSchoolYear(@Param("schoolId") Long schoolId);

    Optional<SchoolClass> findByYear_IdAndLevel_CodeAndName(Long yearId, String levelCode, String name);

    @Query("""
        select sc from SchoolClass sc
        join fetch sc.year y
        join fetch y.school
        where sc.id = :id
        """)
    Optional<SchoolClass> findByIdWithYearAndSchool(@Param("id") Long id);

    @Query("""
        select sc from SchoolClass sc
        join fetch sc.year y
        join fetch y.school
        left join fetch sc.level lv
        left join fetch lv.group
        where sc.id = :id
        """)
    Optional<SchoolClass> findByIdWithContextForPdf(@Param("id") Long id);
}

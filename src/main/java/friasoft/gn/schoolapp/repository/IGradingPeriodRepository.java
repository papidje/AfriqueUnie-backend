package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.GradingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IGradingPeriodRepository extends JpaRepository<GradingPeriod, Long> {

    List<GradingPeriod> findBySchoolClass_IdOrderByStartDateAsc(Long schoolClassId);

    @Query("""
        select gp from GradingPeriod gp
        join fetch gp.schoolClass sc
        join fetch sc.year y
        join fetch y.school
        where gp.id = :id
        """)
    Optional<GradingPeriod> findByIdWithClassAndSchool(@Param("id") Long id);

    @Query("""
        select gp from GradingPeriod gp
        join fetch gp.schoolClass sc
        join fetch sc.year y
        join fetch y.school
        """)
    List<GradingPeriod> findAllWithClassYearAndSchool();
}

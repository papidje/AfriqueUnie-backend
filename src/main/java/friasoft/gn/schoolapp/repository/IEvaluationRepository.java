package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IEvaluationRepository extends JpaRepository<Evaluation, Long> {

    @Query("""
        select e.id from Evaluation e
        join e.classSubject cs
        join cs.schoolClass sc
        join sc.year y
        where y.active = true
          and e.startDate >= :startInclusive
          and e.startDate < :endExclusive
        order by e.id asc
        """)
    Page<Long> findIdsStartingBetweenForActiveYear(
        @Param("startInclusive") LocalDateTime startInclusive,
        @Param("endExclusive") LocalDateTime endExclusive,
        Pageable pageable
    );

    @Query("""
        select e.id from Evaluation e
        join e.classSubject cs
        join cs.schoolClass sc
        join sc.year y
        where y.active = true
          and y.school.id = :schoolId
          and e.startDate >= :startInclusive
          and e.startDate < :endExclusive
        order by e.id asc
        """)
    Page<Long> findIdsStartingBetweenForActiveYearAndSchool(
        @Param("schoolId") Long schoolId,
        @Param("startInclusive") LocalDateTime startInclusive,
        @Param("endExclusive") LocalDateTime endExclusive,
        Pageable pageable
    );

    /** Met à jour la copie dénormal {@code evaluations.coefficient} depuis une nouvelle valeur matière/classe. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Evaluation e set e.coefficient = :coeff where e.classSubject.id = :classSubjectId")
    int bulkSyncCoefficientForClassSubject(
        @Param("classSubjectId") Long classSubjectId,
        @Param("coeff") Double coeff
    );

    long countByGradingPeriod_Id(Long gradingPeriodId);

    @Query("""
        select count(e) from Evaluation e
        join e.classSubject cs
        where cs.schoolClass.id = :classId
        """)
    long countBySchoolClassId(@Param("classId") long classId);

    @Query("""
        select e from Evaluation e
        join fetch e.classSubject cs
        join fetch cs.subject
        join fetch e.gradingPeriod gp
        where cs.schoolClass.id = :classId
        order by e.startDate desc, e.id desc
        """)
    List<Evaluation> findBySchoolClassIdWithDetails(@Param("classId") Long classId);

    @Query("""
        select distinct e from Evaluation e
        join fetch e.classSubject cs
        join fetch cs.subject
        join fetch e.gradingPeriod
        where cs.schoolClass.id = :classId
        and e.startDate <= :rangeEnd
        and e.endDate >= :rangeStart
        order by e.startDate asc, e.id asc
        """)
    List<Evaluation> findForClassOverlappingRange(
        @Param("classId") Long classId,
        @Param("rangeStart") LocalDateTime rangeStart,
        @Param("rangeEnd") LocalDateTime rangeEnd
    );

    @Query("""
        select e from Evaluation e
        join fetch e.classSubject cs
        join fetch cs.subject
        join fetch e.gradingPeriod
        where e.id = :id
        """)
    Optional<Evaluation> findByIdWithDetails(@Param("id") Long id);
}

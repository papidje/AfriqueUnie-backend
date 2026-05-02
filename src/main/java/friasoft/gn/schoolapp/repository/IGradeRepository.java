package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IGradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByEvaluation_IdOrderByStudent_LastNameAscStudent_FirstNameAsc(Long evaluationId);

    Optional<Grade> findByEvaluation_IdAndStudent_Id(Long evaluationId, Long studentId);

    @Query("""
        select g from Grade g
        join fetch g.evaluation e
        join fetch e.gradingPeriod gp
        join fetch e.classSubject cs
        where g.student.id = :studentId
        and gp.id = :periodId
        """)
    List<Grade> findByStudentIdAndGradingPeriodIdWithEvaluations(
        @Param("studentId") Long studentId,
        @Param("periodId") Long periodId
    );

    @Query("""
        select g from Grade g
        join fetch g.student s
        join fetch g.evaluation e
        join fetch e.classSubject cs
        join fetch cs.subject sub
        join fetch e.gradingPeriod gp
        where gp.id = :periodId
        and cs.schoolClass.id = :classId
        """)
    List<Grade> findAllForClassAndPeriod(@Param("classId") Long classId, @Param("periodId") Long periodId);
}

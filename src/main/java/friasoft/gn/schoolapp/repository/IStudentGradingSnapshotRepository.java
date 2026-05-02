package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.StudentGradingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IStudentGradingSnapshotRepository extends JpaRepository<StudentGradingSnapshot, Long> {

    @Query("""
        select s from StudentGradingSnapshot s
        join fetch s.student st
        join fetch s.schoolClass sc
        join fetch s.gradingPeriod gp
        where st.id = :studentId and gp.id = :periodId
        """)
    Optional<StudentGradingSnapshot> findByStudentAndPeriodWithStudentAndClass(
        @Param("studentId") long studentId,
        @Param("periodId") long periodId
    );

    @Query("""
        select s from StudentGradingSnapshot s
        join fetch s.student st
        where s.schoolClass.id = :classId and s.gradingPeriod.id = :periodId
        order by st.lastName asc, st.firstName asc
        """)
    List<StudentGradingSnapshot> findByClassAndPeriodWithStudents(
        @Param("classId") long classId,
        @Param("periodId") long periodId
    );

    long countBySchoolClass_IdAndGradingPeriod_Id(long schoolClassId, long gradingPeriodId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StudentGradingSnapshot s where s.schoolClass.id = :classId and s.gradingPeriod.id = :periodId")
    int deleteByClassAndPeriod(@Param("classId") long classId, @Param("periodId") long periodId);
}

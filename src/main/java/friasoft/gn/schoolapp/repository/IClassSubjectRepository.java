package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    @Query("""
        select cs from ClassSubject cs
        join fetch cs.subject s
        left join fetch cs.teacher
        where cs.schoolClass.id = :classId
        order by s.name
        """)
    List<ClassSubject> findBySchoolClassIdWithSubject(@Param("classId") Long classId);

    @Query("""
        select cs from ClassSubject cs
        join fetch cs.schoolClass sc
        join fetch sc.year y
        join fetch y.school
        join fetch cs.subject
        left join fetch cs.teacher
        where cs.id = :id
        """)
    Optional<ClassSubject> findByIdForManagement(@Param("id") Long id);

    Optional<ClassSubject> findBySchoolClass_IdAndSubject_Id(Long classId, Long subjectId);
}

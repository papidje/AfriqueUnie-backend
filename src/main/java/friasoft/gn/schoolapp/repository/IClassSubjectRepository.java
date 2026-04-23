package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    @Query("""
        select cs.schoolClass.id, count(cs)
        from ClassSubject cs
        where cs.schoolClass.id in :classIds
        group by cs.schoolClass.id
        """)
    List<Object[]> countBySchoolClassIds(@Param("classIds") Collection<Long> classIds);

    @Query("""
        select count(cs)
        from ClassSubject cs
        join cs.schoolClass sc
        join sc.year y
        where y.school.id = :schoolId and y.active = true
        """)
    long countForActiveSchoolYear(@Param("schoolId") Long schoolId);
}

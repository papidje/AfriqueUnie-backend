package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// StudentRepository.java
@Repository
public interface IStudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByLastNameContainingIgnoreCase(String lastName);

    List<Student> findBySchoolClass_Id(Long schoolClassId);

    @Query("""
        select s
        from Student s
        left join fetch s.schoolClass sc
        where sc is not null
        order by s.createdAt desc, s.id desc
        """)
    List<Student> findRecentWithClass(org.springframework.data.domain.Pageable pageable);

    @Query("""
        select count(s)
        from Student s
        join s.schoolClass sc
        join sc.year y
        where y.school.id = :schoolId
        """)
    long countBySchoolId(@Param("schoolId") Long schoolId);

    @Query("""
        select s
        from Student s
        join fetch s.schoolClass sc
        join sc.year y
        where y.school.id = :schoolId
        order by s.createdAt desc, s.id desc
        """)
    List<Student> findRecentWithClassForSchool(
        @Param("schoolId") Long schoolId,
        org.springframework.data.domain.Pageable pageable
    );
}

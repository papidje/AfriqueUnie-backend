package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// StudentRepository.java
@Repository
public interface IStudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByLastNameContainingIgnoreCase(String lastName);

    List<Student> findBySchoolClass_Id(Long schoolClassId);

    List<Student> findBySchoolClass_IdOrderByLastNameAscFirstNameAsc(Long schoolClassId);

    long countBySchoolClass_Id(Long schoolClassId);

    @Query("""
        select s.schoolClass.id, count(s)
        from Student s
        where s.schoolClass.id in :classIds
        group by s.schoolClass.id
        """)
    List<Object[]> countBySchoolClassIds(@Param("classIds") Collection<Long> classIds);

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
        select count(s)
        from Student s
        join s.schoolClass sc
        join sc.year y
        where y.school.id = :schoolId
          and y.active = true
        """)
    long countStudentsActiveSchoolYear(@Param("schoolId") Long schoolId);

    @Query("""
        select s
        from Student s
        join fetch s.schoolClass sc
        join sc.year y
        where y.school.id = :schoolId
          and y.active = true
        order by s.createdAt desc, s.id desc
        """)
    List<Student> findRecentWithClassForSchool(
        @Param("schoolId") Long schoolId,
        org.springframework.data.domain.Pageable pageable
    );

    @Query(
        value = """
            select s from Student s
            join s.schoolClass sc
            join sc.year y
            where y.school.id = :schoolId
            """,
        countQuery = """
            select count(s) from Student s
            join s.schoolClass sc
            join sc.year y
            where y.school.id = :schoolId
            """
    )
    Page<Student> findAllBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query(
        value = """
            select s from Student s
            join s.schoolClass sc
            join sc.year y
            where y.school.id = :schoolId
              and y.active = true
            """,
        countQuery = """
            select count(s) from Student s
            join s.schoolClass sc
            join sc.year y
            where y.school.id = :schoolId
              and y.active = true
            """
    )
    Page<Student> findAllBySchoolIdAndActiveSchoolYear(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query(
        value = """
            select s from Student s
            join s.schoolClass sc
            join sc.year y
            where y.school.id = :schoolId
              and y.active = true
              and sc.id in :classIds
            """,
        countQuery = """
            select count(s) from Student s
            join s.schoolClass sc
            join sc.year y
            where y.school.id = :schoolId
              and y.active = true
              and sc.id in :classIds
            """
    )
    Page<Student> findAllBySchoolIdAndClassIdsAndActiveSchoolYear(
        @Param("schoolId") Long schoolId,
        @Param("classIds") Collection<Long> classIds,
        Pageable pageable
    );

    @Query(
        value = """
            select s from Student s
            join s.schoolClass sc
            where sc.id in :classIds
            """,
        countQuery = """
            select count(s) from Student s
            join s.schoolClass sc
            where sc.id in :classIds
            """
    )
    Page<Student> findAllBySchoolClassIdIn(@Param("classIds") Collection<Long> classIds, Pageable pageable);

    @Query("""
        select distinct s from Student s
        left join fetch s.father
        left join fetch s.mother
        left join fetch s.schoolClass sc
        left join fetch sc.year y
        left join fetch y.school sch
        where s.id = :id
        """)
    Optional<Student> findByIdWithParentsAndClass(@Param("id") Long id);

    @Query("""
        select distinct s from Student s
        left join fetch s.father
        left join fetch s.mother
        where s.schoolClass.id = :classId
        order by s.lastName asc, s.firstName asc
        """)
    List<Student> findBySchoolClassIdWithParents(@Param("classId") Long classId);

    @Query("""
        select distinct s.photoPath
        from Student s
        where s.photoPath is not null
          and trim(s.photoPath) <> ''
        """)
    List<String> findDistinctPhotoPaths();

}

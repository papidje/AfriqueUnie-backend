package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ISubjectRepository extends JpaRepository<Subject, Long> {

    @Query(
        """
        select distinct s from Subject s
        left join fetch s.levelGroups
        where s.school is null or s.school.id = :schoolId
        order by s.name asc
        """
    )
    List<Subject> findCatalogForSchool(@Param("schoolId") Long schoolId);

    @Query(
        """
        select distinct s from Subject s
        where (s.school is null or s.school.id = :schoolId)
          and s.id in (
            select s2.id from Subject s2 join s2.levelGroups g where g.id = :levelGroupId
          )
        order by s.name asc
        """
    )
    List<Subject> findCatalogForSchoolAndLevelGroup(
        @Param("schoolId") Long schoolId,
        @Param("levelGroupId") Long levelGroupId
    );

    @Query(
        """
        select s from Subject s
        left join fetch s.levelGroups
        where s.id = :id and (s.school is null or s.school.id = :schoolId)
        """
    )
    Optional<Subject> findByIdInSchoolCatalog(@Param("id") Long id, @Param("schoolId") Long schoolId);

    @Query(
        """
        select s from Subject s
        left join fetch s.levelGroups
        where s.id = :id
        """
    )
    Optional<Subject> findByIdWithLevelGroups(@Param("id") Long id);

    @Query(
        "select s from Subject s where lower(s.code) = lower(:code) and (s.school is null or s.school.id = :schoolId)"
    )
    List<Subject> findByCodeInCatalogScope(@Param("code") String code, @Param("schoolId") Long schoolId);

    @Query(
        """
        select distinct s from Subject s
        left join fetch s.levelGroups
        where s.school is null
        order by s.name asc
        """
    )
    List<Subject> findGlobalSubjects();

    @Query(
        "select s from Subject s where s.school is null and lower(s.code) = lower(:code)"
    )
    List<Subject> findGlobalByCode(@Param("code") String code);

    @Query("select count(s) from Subject s join s.levelGroups g where g.id = :groupId")
    long countByLevelGroupId(@Param("groupId") Long groupId);

    @Query("select case when count(s) > 0 then true else false end from Subject s join s.levelGroups g where g.id = :groupId")
    boolean existsByLevelGroupId(@Param("groupId") Long groupId);
}

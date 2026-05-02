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
        "select s from Subject s where s.school is null or s.school.id = :schoolId order by s.name asc"
    )
    List<Subject> findCatalogForSchool(@Param("schoolId") Long schoolId);

    @Query(
        "select s from Subject s where s.id = :id and (s.school is null or s.school.id = :schoolId)"
    )
    Optional<Subject> findByIdInSchoolCatalog(@Param("id") Long id, @Param("schoolId") Long schoolId);

    @Query(
        "select s from Subject s where lower(s.code) = lower(:code) and (s.school is null or s.school.id = :schoolId)"
    )
    List<Subject> findByCodeInCatalogScope(@Param("code") String code, @Param("schoolId") Long schoolId);
}

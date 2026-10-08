package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.SubjectAdditionRequest;
import friasoft.gn.schoolapp.entity.school.SubjectAdditionRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ISubjectAdditionRequestRepository extends JpaRepository<SubjectAdditionRequest, Long> {

    @Query("""
        select distinct r from SubjectAdditionRequest r
        join fetch r.school
        join fetch r.requestedBy
        join fetch r.classLevel lv
        join fetch lv.group
        where r.requestedBy.id = :userId
        order by r.createdAt desc
        """)
    List<SubjectAdditionRequest> findAllByRequesterWithRefs(@Param("userId") Long userId);

    @Query("""
        select distinct r from SubjectAdditionRequest r
        join fetch r.school
        join fetch r.requestedBy
        join fetch r.classLevel lv
        join fetch lv.group
        where r.requestedBy.id = :userId and r.status in :statuses
        order by r.createdAt desc
        """)
    List<SubjectAdditionRequest> findAllByRequesterAndStatusInWithRefs(
        @Param("userId") Long userId,
        @Param("statuses") Collection<SubjectAdditionRequestStatus> statuses
    );

    @Query("""
        select distinct r from SubjectAdditionRequest r
        join fetch r.school
        join fetch r.requestedBy
        join fetch r.classLevel lv
        join fetch lv.group
        order by r.createdAt desc
        """)
    List<SubjectAdditionRequest> findAllWithRefs();

    @Query("""
        select distinct r from SubjectAdditionRequest r
        join fetch r.school
        join fetch r.requestedBy
        join fetch r.classLevel lv
        join fetch lv.group
        where r.status in :statuses
        order by r.createdAt desc
        """)
    List<SubjectAdditionRequest> findAllByStatusInWithRefs(
        @Param("statuses") Collection<SubjectAdditionRequestStatus> statuses
    );

    @Query("""
        select r from SubjectAdditionRequest r
        join fetch r.school
        join fetch r.requestedBy
        join fetch r.classLevel lv
        join fetch lv.group
        left join fetch r.createdSubject
        where r.id = :id
        """)
    Optional<SubjectAdditionRequest> findByIdWithRefs(@Param("id") Long id);

    long countByClassLevel_Id(Long classLevelId);

    boolean existsByClassLevel_Id(Long classLevelId);
}

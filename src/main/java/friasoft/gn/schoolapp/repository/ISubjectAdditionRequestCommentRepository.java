package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.SubjectAdditionRequestComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ISubjectAdditionRequestCommentRepository extends JpaRepository<SubjectAdditionRequestComment, Long> {

    @Query("""
        select c from SubjectAdditionRequestComment c
        join fetch c.author
        where c.request.id = :requestId
        order by c.createdAt asc
        """)
    List<SubjectAdditionRequestComment> findByRequestIdOrderByCreatedAtAsc(@Param("requestId") Long requestId);

    @Query("""
        select c.request.id, count(c.id)
        from SubjectAdditionRequestComment c
        where c.request.id in :requestIds
        group by c.request.id
        """)
    List<Object[]> countByRequestIds(@Param("requestIds") Collection<Long> requestIds);
}

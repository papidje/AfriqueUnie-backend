package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.dto.ParentSchoolListRow;
import friasoft.gn.schoolapp.entity.school.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IParentRepository extends JpaRepository<Parent, Long> {
    Optional<Parent> findByTenantIdAndPhone(Long tenantId, String phone);

    @Query("""
        select new friasoft.gn.schoolapp.dto.ParentSchoolListRow(
            p.id, p.lastName, p.firstName, p.phone, p.email, count(distinct s.id),
            sum(case when s.father = p then 1L else 0L end),
            sum(case when s.mother = p then 1L else 0L end)
        )
        from Student s
        join s.schoolClass sc
        join sc.year y
        cross join Parent p
        where (s.father = p or s.mother = p or s.tutor = p)
            and y.school.id = :schoolId
            and y.active = true
        group by p.id, p.lastName, p.firstName, p.phone, p.email
        order by p.lastName asc, p.firstName asc
        """)
    List<ParentSchoolListRow> listParentsWithEnrolledChildrenForSchoolActiveYear(@Param("schoolId") Long schoolId);
}

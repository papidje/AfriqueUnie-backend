package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SchoolRepository extends JpaRepository<School, Long> {

    List<School> findByTenantIdOrderByIdAsc(Long tenantId);

    boolean existsByCity_Id(Long cityId);

    long countByCity_Id(Long cityId);

    long countByCityIsNull();

    @Query("""
        select c.id, c.code, c.name,
               r.id, r.code, r.name,
               c.latitude, c.longitude,
               count(s),
               sum(case when s.isActive = true then 1 else 0 end)
        from School s
        join s.city c
        join c.region r
        group by c.id, c.code, c.name, r.id, r.code, r.name, c.latitude, c.longitude
        order by r.name asc, c.name asc
        """)
    List<Object[]> aggregateSchoolCountsByCity();
}

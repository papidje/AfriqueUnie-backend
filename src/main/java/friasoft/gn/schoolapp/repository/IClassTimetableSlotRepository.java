package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.ClassTimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IClassTimetableSlotRepository extends JpaRepository<ClassTimetableSlot, Long> {

    @Query("""
        select t from ClassTimetableSlot t
        join fetch t.classSubject cs
        join fetch cs.subject s
        left join fetch cs.teacher
        where t.schoolClass.id = :classId
        order by t.dayOfWeek asc, t.slotIndex asc
        """)
    List<ClassTimetableSlot> findBySchoolClassIdWithSubject(@Param("classId") Long classId);

    Optional<ClassTimetableSlot> findBySchoolClass_IdAndDayOfWeekAndSlotIndex(
        Long classId,
        Integer dayOfWeek,
        Integer slotIndex
    );

    void deleteBySchoolClass_IdAndDayOfWeekAndSlotIndex(Long classId, Integer dayOfWeek, Integer slotIndex);
}

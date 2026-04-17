package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;

public interface IPaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
        select coalesce(sum(p.amount), 0d)
        from Payment p
        where p.paymentDate >= :monthStart
          and p.paymentDate < :monthEnd
        """)
    Double sumCollectedAmountBetween(
        @Param("monthStart") LocalDateTime monthStart,
        @Param("monthEnd") LocalDateTime monthEnd
    );

    @Query("""
        select coalesce(sum(p.amount), 0d)
        from Payment p
        join p.studentAccount a
        join a.schoolYear y
        where y.school.id = :schoolId
          and p.paymentDate >= :monthStart
          and p.paymentDate < :monthEnd
        """)
    Double sumCollectedAmountBetweenForSchool(
        @Param("schoolId") Long schoolId,
        @Param("monthStart") LocalDateTime monthStart,
        @Param("monthEnd") LocalDateTime monthEnd
    );

    List<Payment> findByStudentAccount_IdIn(Collection<Long> studentAccountIds);
}


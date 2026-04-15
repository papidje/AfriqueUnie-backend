package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// PaymentRepository.java
@Repository
public interface IPaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByEnrollment_Id(Long enrollmentId);
    List<Payment> findByFee_Id(Long feeId);

    @Query("""
        select coalesce(sum(p.paidAmount), 0)
        from Payment p
        where p.paymentDate >= :monthStart
          and p.paymentDate < :monthEnd
          and p.status <> friasoft.gn.schoolapp.entity.school.Payment.Status.PENDING
    """)
    BigDecimal sumCollectedAmountBetween(LocalDateTime monthStart, LocalDateTime monthEnd);

    @Query("""
        select coalesce(sum(p.paidAmount), 0)
        from Payment p
        where p.enrollment.classRef.year.school.id = :schoolId
          and p.paymentDate >= :monthStart
          and p.paymentDate < :monthEnd
          and p.status <> friasoft.gn.schoolapp.entity.school.Payment.Status.PENDING
    """)
    BigDecimal sumCollectedAmountBySchoolBetween(Long schoolId, LocalDateTime monthStart, LocalDateTime monthEnd);
}

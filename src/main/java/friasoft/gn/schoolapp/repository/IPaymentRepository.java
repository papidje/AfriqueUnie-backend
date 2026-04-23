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

    @Query("""
        select coalesce(sum(p.amount), 0d)
        from Payment p
        join p.studentAccount a
        join a.schoolYear y
        where y.school.id = :schoolId
          and y.active = true
        """)
    Double sumCollectedForActiveSchoolYear(@Param("schoolId") Long schoolId);

    List<Payment> findByStudentAccount_IdIn(Collection<Long> studentAccountIds);

    @Query("""
        select p from Payment p
        join fetch p.studentAccount a
        join fetch a.student s
        left join fetch a.schoolYear y
        left join fetch p.validatedBy v
        where s.id = :studentId
        order by p.paymentDate desc, p.id desc
        """)
    List<Payment> findAllByStudentIdOrderByPaymentDateDesc(@Param("studentId") Long studentId);

    @Query("""
        select p from Payment p
        join fetch p.studentAccount a
        join fetch a.student s
        join fetch a.schoolYear y
        where s.id = :studentId
          and p.receiptReference = :reference
        order by p.id asc
        """)
    List<Payment> findByStudentIdAndReceiptReference(
        @Param("studentId") Long studentId,
        @Param("reference") String reference
    );
}


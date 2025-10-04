package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// PaymentRepository.java
@Repository
public interface IPaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByEnrollment_Id(Long enrollmentId);
    List<Payment> findByFee_Id(Long feeId);
}

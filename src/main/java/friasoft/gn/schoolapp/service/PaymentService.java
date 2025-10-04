package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.Payment;
import friasoft.gn.schoolapp.repository.IPaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// PaymentService.java
@Service
@AllArgsConstructor
public class PaymentService {

    private final IPaymentRepository repository;

    public Payment save(Payment payment) {
        return repository.save(payment);
    }

    public Optional<Payment> findById(Long id) {
        return repository.findById(id);
    }

    public List<Payment> findByEnrollment(Long enrollmentId) {
        return repository.findByEnrollment_Id(enrollmentId);
    }
}


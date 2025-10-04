package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.Payment;
import friasoft.gn.schoolapp.service.PaymentService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@AllArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public List<Payment> getByEnrollment(@PathVariable Long enrollmentId) {
        return service.findByEnrollment(enrollmentId);
    }

    @PostMapping
    public Payment create(@RequestBody Payment payment) {
        return service.save(payment);
    }
}


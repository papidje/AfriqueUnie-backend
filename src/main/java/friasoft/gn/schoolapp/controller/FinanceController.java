package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.StudentPaymentStatusDTO;
import friasoft.gn.schoolapp.dto.StudentPaymentInfoDTO;
import friasoft.gn.schoolapp.dto.FinancePaymentDtos.CreatePaymentRequest;
import friasoft.gn.schoolapp.dto.FinancePaymentDtos.CreatePaymentResponse;
import friasoft.gn.schoolapp.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR','ACCOUNTANT')")
    @GetMapping("/status/{classId}")
    public List<StudentPaymentStatusDTO> getStatusByClass(@PathVariable Long classId) {
        try {
            return financeService.getClassPaymentStatus(classId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR','ACCOUNTANT')")
    @GetMapping("/payment-info/{studentId}")
    public StudentPaymentInfoDTO getPaymentInfo(@PathVariable Long studentId) {
        try {
            return financeService.getStudentPaymentInfo(studentId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_ECOLE','STAFF','DIRECTOR','ACCOUNTANT')")
    @PostMapping("/payments/{studentId}")
    public CreatePaymentResponse createPayment(@PathVariable Long studentId, @RequestBody CreatePaymentRequest request) {
        try {
            return financeService.createStudentPayment(studentId, request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}


package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.Fee;
import friasoft.gn.schoolapp.repository.IFeeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// FeeService.java
@Service
@AllArgsConstructor
public class FeeService {

    private final IFeeRepository repository;

    public Fee save(Fee fee) {
        return repository.save(fee);
    }

    public Optional<Fee> findById(Long id) {
        return repository.findById(id);
    }

    public List<Fee> findByClass(Long classId) {
        return repository.findByClassRef_Id(classId);
    }
}

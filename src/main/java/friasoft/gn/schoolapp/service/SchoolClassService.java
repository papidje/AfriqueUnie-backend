package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// SchoolClassService.java
@Service
@AllArgsConstructor
public class SchoolClassService {

    private final ISchoolClassRepository repository;

    public SchoolClass save(SchoolClass schoolClass) {
        return repository.save(schoolClass);
    }

    public Optional<SchoolClass> findById(Long id) {
        return repository.findById(id);
    }

    public List<SchoolClass> findByYear(Long yearId) {
        return repository.findByYear_Id(yearId);
    }
}

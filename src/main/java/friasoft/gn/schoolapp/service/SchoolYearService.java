package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.repository.ISchoolYearRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// SchoolYearService.java
@Service
@AllArgsConstructor
public class SchoolYearService {

    private final ISchoolYearRepository repository;

    public SchoolYear save(SchoolYear year) {
        return repository.save(year);
    }

    public Optional<SchoolYear> findById(Long id) {
        return repository.findById(id);
    }

    public List<SchoolYear> findBySchool(Long schoolId) {
        return repository.findBySchoolId(schoolId);
    }

    public Optional<SchoolYear> getActiveYear(Long schoolId) {
        return repository.findBySchoolIdAndActiveTrue(schoolId);
    }
}

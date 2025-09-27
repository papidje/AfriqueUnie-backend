package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.School;
import friasoft.gn.schoolapp.entity.User;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class SchoolService {
    private final SchoolRepository schoolRepository;

    public void create(School school) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        school.setCreated_at(Instant.now());
        this.schoolRepository.save(school);
    }

    public List<School> getAll() {
        List<School> actualList = new ArrayList<>();
        this.schoolRepository.findAll().iterator().forEachRemaining(actualList::add);
        return actualList;
    }

    public School update(Long schoolId, School dto) {
        School school = this.schoolRepository.findById(schoolId).orElseThrow(() -> new RuntimeException("School not found"));
        school.setName(dto.getName());
        school.setUpdated_at(Instant.now());
        return this.schoolRepository.save(school);
    }

    public void delete(Long schoolId) {

    }
}

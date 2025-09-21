package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.School;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class SchoolService {
    private final SchoolRepository schoolRepository;

    public void create(School school) {
        //User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        this.schoolRepository.save(school);
    }

    public List<School> getAll() {
        List<School> actualList = new ArrayList<School>();
        this.schoolRepository.findAll().iterator().forEachRemaining(actualList::add);
        return actualList;
    }
}

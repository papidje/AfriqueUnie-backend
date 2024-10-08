package friasoft.gn.schoolapp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import friasoft.gn.schoolapp.entity.School;
import friasoft.gn.schoolapp.entity.User;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class SchoolService {
    private final SchoolRepository schoolRepository;

    public void create(School school) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        this.schoolRepository.save(school);
    }

    public List<School> getAll() {
        List<School> actualList = new ArrayList<School>();
        this.schoolRepository.findAll().iterator().forEachRemaining(actualList::add);
        return actualList;
    }
}

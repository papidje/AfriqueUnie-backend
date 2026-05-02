package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.repository.IClassLevelRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// ClassLevelService.java
@Service
@AllArgsConstructor
public class ClassLevelService {

    private final IClassLevelRepository repository;

    public ClassLevel save(ClassLevel level) {
        return repository.save(level);
    }

    public Optional<ClassLevel> findById(Long id) {
        return repository.findById(id);
    }

    public List<ClassLevel> findByGroup(String groupCode) {
        return repository.findByGroup_Code(groupCode);
    }

    public List<ClassLevel> findAll() {
        return repository.findAll();
    }
}

package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.repository.IClassLevelGroupRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ClassLevelGroupService {
    private final IClassLevelGroupRepository repository;

    public List<ClassLevelGroup> findAll() {
        return repository.findAll();
    }

    public ClassLevelGroup save(ClassLevelGroup group) {
        return repository.save(group);
    }
}

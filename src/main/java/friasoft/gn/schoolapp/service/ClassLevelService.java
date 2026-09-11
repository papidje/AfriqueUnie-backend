package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.repository.IClassLevelRepository;
import friasoft.gn.schoolapp.util.ClassLevelOrdering;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    @Transactional(readOnly = true)
    public List<ClassLevel> findByGroup(String groupCode) {
        return repository.findByGroup_Code(groupCode).stream()
            .sorted(ClassLevelOrdering.classLevelComparator())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ClassLevel> findAll() {
        return repository.findAllWithGroup().stream()
            .sorted(ClassLevelOrdering.classLevelComparator())
            .toList();
    }
}

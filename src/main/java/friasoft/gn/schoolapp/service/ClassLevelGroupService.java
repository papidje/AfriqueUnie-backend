package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.repository.IClassLevelGroupRepository;
import friasoft.gn.schoolapp.util.ClassLevelOrdering;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class ClassLevelGroupService {
    private final IClassLevelGroupRepository repository;

    public List<ClassLevelGroup> findAll() {
        return repository.findAll().stream()
            .sorted(Comparator
                .comparingInt((ClassLevelGroup g) -> ClassLevelOrdering.groupSortKey(g.getCode()))
                .thenComparing(g -> g.getCode() != null ? g.getCode() : "", String::compareToIgnoreCase))
            .toList();
    }

    public ClassLevelGroup save(ClassLevelGroup group) {
        return repository.save(group);
    }
}

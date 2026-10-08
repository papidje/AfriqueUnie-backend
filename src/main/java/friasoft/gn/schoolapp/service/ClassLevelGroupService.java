package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ClassLevelDtos.ClassLevelGroupRequest;
import friasoft.gn.schoolapp.dto.ClassLevelDtos.ClassLevelGroupResponse;
import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.repository.IClassLevelGroupRepository;
import friasoft.gn.schoolapp.repository.IClassLevelRepository;
import friasoft.gn.schoolapp.repository.ISubjectRepository;
import friasoft.gn.schoolapp.util.ClassLevelOrdering;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class ClassLevelGroupService {

    private final IClassLevelGroupRepository repository;
    private final IClassLevelRepository classLevelRepository;
    private final ISubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<ClassLevelGroup> findAll() {
        return repository.findAll().stream()
            .sorted(ClassLevelOrdering.classLevelGroupComparator())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ClassLevelGroupResponse> listAllResponsesForAdmin() {
        return findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ClassLevelGroupResponse create(ClassLevelGroupRequest request) {
        ClassLevelGroup group = new ClassLevelGroup();
        applyRequest(group, request, true);
        return toResponse(repository.save(group));
    }

    @Transactional
    public ClassLevelGroupResponse update(Long id, ClassLevelGroupRequest request) {
        ClassLevelGroup group = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cycle scolaire introuvable."));
        applyRequest(group, request, false);
        return toResponse(repository.save(group));
    }

    @Transactional
    public void delete(Long id) {
        ClassLevelGroup group = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cycle scolaire introuvable."));
        if (classLevelRepository.existsByGroup_Id(id)) {
            throw new IllegalStateException(
                "Impossible de supprimer : des niveaux sont rattachés à ce cycle."
            );
        }
        if (subjectRepository.existsByLevelGroupId(id)) {
            throw new IllegalStateException(
                "Impossible de supprimer : des matières sont rattachées à ce cycle."
            );
        }
        repository.delete(group);
    }

    private void applyRequest(ClassLevelGroup group, ClassLevelGroupRequest request, boolean creating) {
        if (request == null) {
            throw new IllegalArgumentException("Corps de requête obligatoire.");
        }
        String code = request.code() != null ? request.code().trim().toUpperCase(Locale.ROOT) : "";
        String name = request.name() != null ? request.name().trim() : "";
        if (code.isEmpty() || code.length() > 20) {
            throw new IllegalArgumentException("Code obligatoire (max 20 caractères).");
        }
        if (name.isEmpty() || name.length() > 100) {
            throw new IllegalArgumentException("Nom obligatoire (max 100 caractères).");
        }
        boolean codeTaken = creating
            ? repository.existsByCodeIgnoreCase(code)
            : repository.existsByCodeIgnoreCaseAndIdNot(code, group.getId());
        if (codeTaken) {
            throw new IllegalStateException("Un cycle avec ce code existe déjà.");
        }
        group.setCode(code);
        group.setName(name);
        if (request.sortOrder() != null) {
            group.setSortOrder(request.sortOrder());
        } else if (creating) {
            int max = repository.findAll().stream()
                .mapToInt(ClassLevelGroup::getSortOrder)
                .max()
                .orElse(0);
            group.setSortOrder(max + 10);
        }
    }

    private ClassLevelGroupResponse toResponse(ClassLevelGroup group) {
        long levels = classLevelRepository.countByGroup_Id(group.getId());
        return new ClassLevelGroupResponse(
            group.getId(),
            group.getCode(),
            group.getName(),
            group.getSortOrder(),
            levels
        );
    }
}

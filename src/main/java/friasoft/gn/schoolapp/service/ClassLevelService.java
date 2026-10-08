package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ClassLevelDtos.ClassLevelRequest;
import friasoft.gn.schoolapp.dto.ClassLevelDtos.ClassLevelResponse;
import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.repository.IClassLevelGroupRepository;
import friasoft.gn.schoolapp.repository.IClassLevelRepository;
import friasoft.gn.schoolapp.repository.IFeeStructureRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import friasoft.gn.schoolapp.repository.ISubjectAdditionRequestRepository;
import friasoft.gn.schoolapp.util.ClassLevelOrdering;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ClassLevelService {

    private final IClassLevelRepository repository;
    private final IClassLevelGroupRepository groupRepository;
    private final ISchoolClassRepository schoolClassRepository;
    private final IFeeStructureRepository feeStructureRepository;
    private final ISubjectAdditionRequestRepository subjectAdditionRequestRepository;

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

    @Transactional(readOnly = true)
    public List<ClassLevelResponse> listAllResponsesForAdmin() {
        return findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ClassLevelResponse create(ClassLevelRequest request) {
        ClassLevel level = new ClassLevel();
        applyRequest(level, request, true);
        return toResponse(repository.save(level));
    }

    @Transactional
    public ClassLevelResponse update(Long id, ClassLevelRequest request) {
        ClassLevel level = repository.findByIdWithGroup(id)
            .orElseThrow(() -> new IllegalArgumentException("Niveau scolaire introuvable."));
        applyRequest(level, request, false);
        return toResponse(repository.save(level));
    }

    @Transactional
    public void delete(Long id) {
        ClassLevel level = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Niveau scolaire introuvable."));
        long usage = usageCount(id);
        if (usage > 0) {
            throw new IllegalStateException(
                "Impossible de supprimer : ce niveau est utilisé par " + usage
                    + " élément(s) (classes, barèmes ou demandes)."
            );
        }
        repository.delete(level);
    }

    private void applyRequest(ClassLevel level, ClassLevelRequest request, boolean creating) {
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
        if (request.groupId() == null) {
            throw new IllegalArgumentException("Cycle scolaire obligatoire.");
        }
        ClassLevelGroup group = groupRepository.findById(request.groupId())
            .orElseThrow(() -> new IllegalArgumentException("Cycle scolaire introuvable."));
        boolean codeTaken = creating
            ? repository.existsByCodeIgnoreCase(code)
            : repository.existsByCodeIgnoreCaseAndIdNot(code, level.getId());
        if (codeTaken) {
            throw new IllegalStateException("Un niveau avec ce code existe déjà.");
        }
        level.setCode(code);
        level.setName(name);
        level.setGroup(group);
        if (request.sortOrder() != null) {
            level.setSortOrder(request.sortOrder());
        } else if (creating) {
            int max = repository.findByGroup_Code(group.getCode()).stream()
                .mapToInt(ClassLevel::getSortOrder)
                .max()
                .orElse(0);
            level.setSortOrder(max + 10);
        }
    }

    private long usageCount(Long levelId) {
        return schoolClassRepository.countByLevel_Id(levelId)
            + feeStructureRepository.countByClassLevel_Id(levelId)
            + subjectAdditionRequestRepository.countByClassLevel_Id(levelId);
    }

    private ClassLevelResponse toResponse(ClassLevel level) {
        ClassLevelGroup group = level.getGroup();
        return new ClassLevelResponse(
            level.getId(),
            level.getCode(),
            level.getName(),
            level.getSortOrder(),
            group != null ? group.getId() : null,
            group != null ? group.getCode() : null,
            group != null ? group.getName() : null,
            usageCount(level.getId())
        );
    }
}

package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.Subject;
import friasoft.gn.schoolapp.repository.ISubjectRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SubjectService {

    private final ISubjectRepository repository;

    public List<Subject> findAll() {
        return repository.findAll();
    }

    public Optional<Subject> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Subject save(Subject subject) {
        if (subject.getCode() != null) {
            subject.setCode(subject.getCode().trim());
        }
        if (subject.getName() != null) {
            subject.setName(subject.getName().trim());
        }
        if (subject.getCode() == null || subject.getCode().isEmpty()) {
            throw new IllegalArgumentException("Le code est obligatoire.");
        }
        if (subject.getName() == null || subject.getName().isEmpty()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        Optional<Subject> sameCode = repository.findByCodeIgnoreCase(subject.getCode());
        if (subject.getId() == null) {
            if (sameCode.isPresent()) {
                throw new IllegalArgumentException("Une matière avec ce code existe déjà.");
            }
        } else if (sameCode.isPresent() && !sameCode.get().getId().equals(subject.getId())) {
            throw new IllegalArgumentException("Une matière avec ce code existe déjà.");
        }
        try {
            return repository.save(subject);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Code matière déjà utilisé.", e);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        try {
            repository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Impossible de supprimer : la matière est affectée à une ou plusieurs classes.", e);
        }
    }
}

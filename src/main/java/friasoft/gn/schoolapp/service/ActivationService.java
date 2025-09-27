package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.Activation;
import friasoft.gn.schoolapp.repository.IActivationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class ActivationService {

    private IActivationRepository iActivationRepository;

    public List<Activation> getAll() {
        return this.iActivationRepository.findAll();
    }

//    public List<Activation> findBySchool(short schoolId) {
//        return this.iActivationRepository.findAllBySchoolId(schoolId);
//    }

    public Optional<Activation> findByCode(String code) {
        return this.iActivationRepository.findByCode(code);
    }
}

package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.Region;
import friasoft.gn.schoolapp.repository.IRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final IRegionRepository regionRepository;

    @Transactional(readOnly = true)
    public List<Region> listActive() {
        return regionRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Region> listAllForAdmin() {
        return regionRepository.findAllByOrderByNameAsc();
    }
}

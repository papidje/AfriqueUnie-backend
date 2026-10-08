package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.RegionDtos.RegionRequest;
import friasoft.gn.schoolapp.dto.RegionDtos.RegionResponse;
import friasoft.gn.schoolapp.entity.school.Region;
import friasoft.gn.schoolapp.repository.ICityRepository;
import friasoft.gn.schoolapp.repository.IRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final IRegionRepository regionRepository;
    private final ICityRepository cityRepository;

    @Transactional(readOnly = true)
    public List<Region> listActive() {
        return regionRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Region> listAllForAdmin() {
        return regionRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<RegionResponse> listAllResponsesForAdmin() {
        return regionRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public RegionResponse create(RegionRequest request) {
        Region region = new Region();
        applyRequest(region, request, true);
        return toResponse(regionRepository.save(region));
    }

    @Transactional
    public RegionResponse update(Long id, RegionRequest request) {
        Region region = regionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Région introuvable."));
        applyRequest(region, request, false);
        return toResponse(regionRepository.save(region));
    }

    @Transactional
    public RegionResponse setActive(Long id, boolean active) {
        Region region = regionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Région introuvable."));
        region.setActive(active);
        return toResponse(regionRepository.save(region));
    }

    @Transactional
    public void delete(Long id) {
        Region region = regionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Région introuvable."));
        if (cityRepository.existsByRegion_Id(id)) {
            throw new IllegalStateException(
                "Impossible de supprimer : des villes sont rattachées à cette région. Désactivez-la plutôt."
            );
        }
        regionRepository.delete(region);
    }

    private void applyRequest(Region region, RegionRequest request, boolean creating) {
        if (request == null) {
            throw new IllegalArgumentException("Corps de requête obligatoire.");
        }
        String code = request.code() != null ? request.code().trim().toUpperCase(Locale.ROOT) : "";
        String name = request.name() != null ? request.name().trim() : "";
        if (code.isEmpty() || code.length() > 32) {
            throw new IllegalArgumentException("Code obligatoire (max 32 caractères).");
        }
        if (name.isEmpty() || name.length() > 120) {
            throw new IllegalArgumentException("Nom obligatoire (max 120 caractères).");
        }
        boolean codeTaken = creating
            ? regionRepository.existsByCodeIgnoreCase(code)
            : regionRepository.existsByCodeIgnoreCaseAndIdNot(code, region.getId());
        if (codeTaken) {
            throw new IllegalStateException("Une région avec ce code existe déjà.");
        }
        region.setCode(code);
        region.setName(name);
        if (request.active() != null) {
            region.setActive(request.active());
        } else if (creating) {
            region.setActive(true);
        }
    }

    private RegionResponse toResponse(Region region) {
        long cities = cityRepository.countByRegion_Id(region.getId());
        return new RegionResponse(
            region.getId(),
            region.getCode(),
            region.getName(),
            region.isActive(),
            cities
        );
    }
}

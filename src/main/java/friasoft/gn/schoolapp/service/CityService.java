package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.CityDtos.CityRequest;
import friasoft.gn.schoolapp.dto.CityDtos.CityResponse;
import friasoft.gn.schoolapp.entity.school.City;
import friasoft.gn.schoolapp.entity.school.Region;
import friasoft.gn.schoolapp.repository.ICityRepository;
import friasoft.gn.schoolapp.repository.IRegionRepository;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CityService {

    private final ICityRepository cityRepository;
    private final IRegionRepository regionRepository;
    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public List<City> listActive() {
        return cityRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<CityResponse> listAllForAdmin() {
        return cityRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public City requireActiveCity(Long cityId) {
        City city = cityRepository.findById(cityId)
            .orElseThrow(() -> new IllegalArgumentException("Ville introuvable."));
        if (!city.isActive()) {
            throw new IllegalArgumentException("Cette ville n’est plus active.");
        }
        return city;
    }

    @Transactional
    public CityResponse create(CityRequest request) {
        City city = new City();
        applyRequest(city, request, true);
        return toResponse(cityRepository.save(city));
    }

    @Transactional
    public CityResponse update(Long id, CityRequest request) {
        City city = cityRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Ville introuvable."));
        applyRequest(city, request, false);
        return toResponse(cityRepository.save(city));
    }

    @Transactional
    public CityResponse setActive(Long id, boolean active) {
        City city = cityRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Ville introuvable."));
        city.setActive(active);
        return toResponse(cityRepository.save(city));
    }

    @Transactional
    public void delete(Long id) {
        City city = cityRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Ville introuvable."));
        if (schoolRepository.existsByCity_Id(id)) {
            throw new IllegalStateException(
                "Impossible de supprimer : des établissements sont rattachés à cette ville. Désactivez-la plutôt."
            );
        }
        cityRepository.delete(city);
    }

    private void applyRequest(City city, CityRequest request, boolean creating) {
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
        if (request.latitude() == null || request.longitude() == null) {
            throw new IllegalArgumentException("Latitude et longitude obligatoires.");
        }
        if (request.latitude() < -90 || request.latitude() > 90
            || request.longitude() < -180 || request.longitude() > 180) {
            throw new IllegalArgumentException("Coordonnées GPS invalides.");
        }
        if (request.regionId() == null) {
            throw new IllegalArgumentException("La région est obligatoire.");
        }
        Region region = regionRepository.findById(request.regionId())
            .orElseThrow(() -> new IllegalArgumentException("Région introuvable."));
        if (!region.isActive()) {
            throw new IllegalArgumentException("Cette région n’est plus active.");
        }
        boolean codeTaken = creating
            ? cityRepository.existsByCodeIgnoreCase(code)
            : cityRepository.existsByCodeIgnoreCaseAndIdNot(code, city.getId());
        if (codeTaken) {
            throw new IllegalStateException("Une ville avec ce code existe déjà.");
        }
        city.setCode(code);
        city.setName(name);
        city.setRegion(region);
        city.setLatitude(request.latitude());
        city.setLongitude(request.longitude());
        if (request.active() != null) {
            city.setActive(request.active());
        } else if (creating) {
            city.setActive(true);
        }
    }

    private CityResponse toResponse(City city) {
        long schools = schoolRepository.countByCity_Id(city.getId());
        Region region = city.getRegion();
        return new CityResponse(
            city.getId(),
            city.getCode(),
            city.getName(),
            region != null ? region.getId() : null,
            region != null ? region.getCode() : null,
            region != null ? region.getName() : null,
            city.getLatitude(),
            city.getLongitude(),
            city.isActive(),
            schools
        );
    }
}

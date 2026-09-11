package friasoft.gn.schoolapp.dto;

public final class CityDtos {

    private CityDtos() {}

    public record CityRequest(
        String code,
        String name,
        Long regionId,
        Double latitude,
        Double longitude,
        Boolean active
    ) {}

    public record CityResponse(
        Long id,
        String code,
        String name,
        Long regionId,
        String regionCode,
        String regionName,
        Double latitude,
        Double longitude,
        boolean active,
        long schoolCount
    ) {}
}

package friasoft.gn.schoolapp.dto;

public final class RegionDtos {

    private RegionDtos() {}

    public record RegionRequest(
        String code,
        String name,
        Boolean active
    ) {}

    public record RegionResponse(
        Long id,
        String code,
        String name,
        boolean active,
        long cityCount
    ) {}
}

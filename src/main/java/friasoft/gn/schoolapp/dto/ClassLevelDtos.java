package friasoft.gn.schoolapp.dto;

public final class ClassLevelDtos {

    private ClassLevelDtos() {}

    public record ClassLevelGroupRequest(
        String code,
        String name,
        Integer sortOrder
    ) {}

    public record ClassLevelGroupResponse(
        Long id,
        String code,
        String name,
        int sortOrder,
        long levelCount
    ) {}

    public record ClassLevelRequest(
        String code,
        String name,
        Long groupId,
        Integer sortOrder
    ) {}

    public record ClassLevelResponse(
        Long id,
        String code,
        String name,
        int sortOrder,
        Long groupId,
        String groupCode,
        String groupName,
        long usageCount
    ) {}
}

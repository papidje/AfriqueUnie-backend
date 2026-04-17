package friasoft.gn.schoolapp.dto;

public final class ParentDtos {
    private ParentDtos() {}

    public record ParentWriteRequest(
        String lastName,
        String firstName,
        String phone,
        String email,
        String profession,
        String address
    ) {}

    public record ParentResponse(
        Long id,
        Long tenantId,
        String lastName,
        String firstName,
        String phone,
        String email,
        String profession,
        String address
    ) {}
}

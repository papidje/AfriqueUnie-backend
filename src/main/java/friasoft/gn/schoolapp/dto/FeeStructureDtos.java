package friasoft.gn.schoolapp.dto;

public final class FeeStructureDtos {
    private FeeStructureDtos() {}

    public record FeeStructureRequest(
        Long classLevelId,
        Long schoolYearId,
        Double registrationFee,
        Double reRegistrationFee,
        Double monthlyTuitionFee,
        Double suppliesFee,
        Boolean suppliesColumnEnabled,
        String currency
    ) {}

    public record FeeStructureResponse(
        Long id,
        Long tenantId,
        Long classLevelId,
        String classLevelCode,
        String classLevelName,
        Long schoolYearId,
        String schoolYearLabel,
        Double registrationFee,
        Double reRegistrationFee,
        Double monthlyTuitionFee,
        Double suppliesFee,
        Boolean suppliesColumnEnabled,
        String currency
    ) {}
}

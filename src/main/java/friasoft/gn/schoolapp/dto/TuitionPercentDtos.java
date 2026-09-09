package friasoft.gn.schoolapp.dto;

public final class TuitionPercentDtos {
    private TuitionPercentDtos() {}

    public record UpdateTuitionPayablePercentRequest(Double tuitionPayablePercent) {}

    public record TuitionPayablePercentResponse(
        Long studentId,
        Long studentAccountId,
        Double tuitionPayablePercent,
        boolean locked
    ) {}
}

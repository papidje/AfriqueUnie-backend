package friasoft.gn.schoolapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import friasoft.gn.schoolapp.entity.school.PeriodType;

/**
 * Liste classes année active : identité + effectif inscrit + nombre de matières affectées.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SchoolClassOverviewResponse(
    Long id,
    String name,
    Integer capacity,
    PeriodType periodType,
    SchoolYearRef year,
    ClassLevelRef level,
    long enrolledStudentCount,
    long subjectCount
) {
    public record SchoolYearRef(Long id, String label) {}

    public record ClassLevelRef(Long id, String code, String name, ClassLevelGroupRef group) {}

    public record ClassLevelGroupRef(Long id, String code, String name) {}
}

package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.school.GradingPeriod;
import friasoft.gn.schoolapp.entity.school.PeriodType;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.repository.IGradingPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradingPeriodService {

    private final IGradingPeriodRepository repository;

    /**
     * Découpe l’année scolaire en 3 trimestres ou 2 semestres (périodes contiguës, dernière se termine à
     * {@code year.endDate}).
     */
    @Transactional
    public void createForNewClass(SchoolClass schoolClass, SchoolYear year) {
        PeriodType type = schoolClass.getPeriodType() != null ? schoolClass.getPeriodType() : PeriodType.TRIMESTER;
        List<GradingPeriod> rows = buildPeriods(schoolClass, year, type);
        repository.saveAll(rows);
    }

    static List<GradingPeriod> buildPeriods(SchoolClass schoolClass, SchoolYear year, PeriodType type) {
        LocalDate yearStart = year.getStartDate();
        LocalDate yearEnd = year.getEndDate();
        if (yearStart == null || yearEnd == null) {
            throw new IllegalArgumentException("L’année scolaire doit avoir des dates de début et de fin.");
        }
        if (yearEnd.isBefore(yearStart)) {
            throw new IllegalArgumentException("La date de fin d’année est avant la date de début.");
        }

        int n = type == PeriodType.SEMESTER ? 2 : 3;
        long totalDays = ChronoUnit.DAYS.between(yearStart, yearEnd) + 1;
        if (totalDays < n) {
            throw new IllegalArgumentException(
                "L’année scolaire est trop courte (" + totalDays + " j.) pour " + n + " périodes."
            );
        }

        long base = totalDays / n;
        long extra = totalDays % n;

        List<GradingPeriod> out = new ArrayList<>(n);
        LocalDate cursor = yearStart;
        for (int i = 0; i < n; i++) {
            long daysInSegment = base + (i < extra ? 1 : 0);
            LocalDate segStart = cursor;
            LocalDate segEnd = i == n - 1
                ? yearEnd
                : segStart.plusDays(daysInSegment - 1);
            GradingPeriod gp = new GradingPeriod();
            gp.setSchoolClass(schoolClass);
            gp.setTenantId(schoolClass.getTenantId());
            gp.setName(periodName(i + 1, type));
            gp.setStartDate(segStart);
            gp.setEndDate(segEnd);
            out.add(gp);
            cursor = segEnd.plusDays(1);
        }
        return out;
    }

    private static String periodName(int index1Based, PeriodType type) {
        if (type == PeriodType.SEMESTER) {
            return "Semestre " + index1Based;
        }
        return "Trimestre " + index1Based;
    }
}

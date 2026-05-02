package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.EvaluationDtos.GradingPeriodDateItem;
import friasoft.gn.schoolapp.dto.EvaluationDtos.UpdateGradingPeriodsScheduleRequest;
import friasoft.gn.schoolapp.entity.school.GradingPeriod;
import friasoft.gn.schoolapp.entity.school.PeriodType;
import friasoft.gn.schoolapp.entity.school.SchoolClass;
import friasoft.gn.schoolapp.entity.school.SchoolYear;
import friasoft.gn.schoolapp.repository.IEvaluationRepository;
import friasoft.gn.schoolapp.repository.IGradingPeriodRepository;
import friasoft.gn.schoolapp.repository.ISchoolClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration des périodes de notation (dates, type trimestre / semestre) avec verrouillage si évaluations.
 */
@Service
@RequiredArgsConstructor
public class GradingPeriodSettingsService {

    private final ISchoolClassRepository schoolClassRepository;
    private final IGradingPeriodRepository gradingPeriodRepository;
    private final IEvaluationRepository evaluationRepository;
    private final GradingPeriodService gradingPeriodService;
    private final SchoolService schoolService;

    @Transactional
    public void updatePeriodType(long classId, PeriodType newType) {
        if (newType == null) {
            throw new IllegalArgumentException("periodType est obligatoire.");
        }
        SchoolClass sc = loadClassWithYear(classId);
        schoolService.assertCurrentUserCanAccessSchool(sc.getYear().getSchool().getId());
        if (sc.getPeriodType() == newType) {
            return;
        }
        if (evaluationRepository.countBySchoolClassId(classId) > 0) {
            throw new IllegalStateException(
                "Des évaluations existent déjà pour cette classe : le type de période ne peut plus être modifié."
            );
        }
        List<GradingPeriod> existing = gradingPeriodRepository.findBySchoolClass_IdOrderByStartDateAsc(classId);
        gradingPeriodRepository.deleteAll(existing);
        sc.setPeriodType(newType);
        schoolClassRepository.save(sc);
        gradingPeriodService.createForNewClass(sc, sc.getYear());
    }

    @Transactional
    public void updateGradingPeriodsSchedule(long classId, UpdateGradingPeriodsScheduleRequest request) {
        if (request == null || request.periods() == null || request.periods().isEmpty()) {
            throw new IllegalArgumentException("Liste de périodes obligatoire.");
        }
        SchoolClass sc = loadClassWithYear(classId);
        schoolService.assertCurrentUserCanAccessSchool(sc.getYear().getSchool().getId());
        SchoolYear year = sc.getYear();
        LocalDate yearStart = year.getStartDate();
        LocalDate yearEnd = year.getEndDate();
        if (yearStart == null || yearEnd == null) {
            throw new IllegalStateException("L’année scolaire doit avoir des dates de début et de fin.");
        }

        List<GradingPeriod> existing = new ArrayList<>(
            gradingPeriodRepository.findBySchoolClass_IdOrderByStartDateAsc(classId)
        );
        if (existing.size() != request.periods().size()) {
            throw new IllegalArgumentException("Le nombre de périodes ne correspond pas à la configuration actuelle.");
        }
        Map<Long, GradingPeriod> byId = new HashMap<>();
        for (GradingPeriod gp : existing) {
            byId.put(gp.getId(), gp);
        }

        for (GradingPeriodDateItem item : request.periods()) {
            GradingPeriod gp = byId.get(item.id());
            if (gp == null) {
                throw new IllegalArgumentException("Période inconnue pour cette classe : " + item.id());
            }
            if (!Objects.equals(gp.getSchoolClass().getId(), classId)) {
                throw new IllegalArgumentException("Période " + item.id() + " n’appartient pas à cette classe.");
            }
            long nEval = evaluationRepository.countByGradingPeriod_Id(gp.getId());
            if (nEval > 0) {
                if (!gp.getStartDate().equals(item.startDate()) || !gp.getEndDate().equals(item.endDate())) {
                    throw new IllegalStateException(
                        "Des évaluations existent sur la période « " + gp.getName() + " » : "
                            + "les dates ne peuvent pas être modifiées."
                    );
                }
            } else {
                validateDatesInYear(item.startDate(), item.endDate(), yearStart, yearEnd);
                gp.setStartDate(item.startDate());
                gp.setEndDate(item.endDate());
            }
            if (item.name() != null && !item.name().isBlank() && nEval == 0) {
                gp.setName(item.name().trim());
            }
        }

        List<GradingPeriod> sortedGps = new ArrayList<>(byId.values());
        sortedGps.sort(Comparator.comparing(GradingPeriod::getStartDate));
        validateContiguousChain(yearStart, yearEnd, sortedGps);
        gradingPeriodRepository.saveAll(sortedGps);
    }

    private void validateContiguousChain(LocalDate yearStart, LocalDate yearEnd, List<GradingPeriod> sorted) {
        if (sorted.isEmpty()) {
            return;
        }
        for (int i = 0; i < sorted.size(); i++) {
            GradingPeriod p = sorted.get(i);
            if (i == 0) {
                if (!p.getStartDate().equals(yearStart)) {
                    throw new IllegalArgumentException("La première période doit commencer le " + yearStart + ".");
                }
            } else {
                LocalDate expectStart = sorted.get(i - 1).getEndDate().plusDays(1);
                if (!p.getStartDate().equals(expectStart)) {
                    throw new IllegalArgumentException(
                        "Périodes non contiguës : après « " + sorted.get(i - 1).getName() + " » (" + sorted.get(
                            i - 1
                        ).getEndDate() + "), la période « " + p.getName() + " » devrait commencer le " + expectStart
                            + "."
                    );
                }
            }
            if (i == sorted.size() - 1) {
                if (!p.getEndDate().equals(yearEnd)) {
                    throw new IllegalArgumentException("La dernière période doit se terminer le " + yearEnd + ".");
                }
            }
            if (p.getStartDate().isAfter(p.getEndDate())) {
                throw new IllegalArgumentException("Période « " + p.getName() + " » : la date de début est après la fin.");
            }
        }
    }

    private static void validateDatesInYear(LocalDate start, LocalDate end, LocalDate yearStart, LocalDate yearEnd) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Dates de période obligatoires.");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La fin de période est avant le début.");
        }
        if (start.isBefore(yearStart) || end.isAfter(yearEnd)) {
            throw new IllegalArgumentException("Les dates doivent rester à l’intérieur de l’année scolaire active.");
        }
    }

    private SchoolClass loadClassWithYear(long classId) {
        return schoolClassRepository
            .findByIdWithYearAndSchool(classId)
            .orElseThrow(() -> new IllegalArgumentException("Classe introuvable."));
    }
}

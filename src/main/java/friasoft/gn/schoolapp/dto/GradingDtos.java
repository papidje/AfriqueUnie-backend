package friasoft.gn.schoolapp.dto;

import java.time.Instant;
import java.util.List;

/**
 * Résultats de calcul de moyenne périodique (contrôle continu + composition).
 */
public final class GradingDtos {

    private GradingDtos() {
    }

    /**
     * @param continuousAverage  Moyenne pondérée (coef.) des interros / devoirs / quiz, ramenée sur /20. Absente si aucune note.
     * @param compositionAverage Moyenne pondérée des compositions, sur /20. Absente si aucune composition notée.
     * @param periodFinalAverage Moyenne de période combinant le contrôle continu et la composition (voir poids de composition).
     * @param compositionWeight  Part de la composition dans la moyenne finale (0 = uniquement le continu, 1 = uniquement la composition, ex. 0,5 = 50 %).
     * @param usedGradesCount    Nombre de notes retenues pour le calcul du continu.
     * @param usedCompositionCount Nombre de notes de composition retenues.
     */
    public record PeriodAverageBreakdown(
        Long studentId,
        Long gradingPeriodId,
        String gradingPeriodName,
        Double continuousAverage,
        Double compositionAverage,
        Double periodFinalAverage,
        double compositionWeight,
        int usedGradesCount,
        int usedCompositionCount
    ) {
    }

    public record ClassSubjectColumn(
        long classSubjectId,
        String subjectCode,
        String subjectName,
        double coefficient
    ) {
    }

    /**
     * @param averages  Une valeur par colonne (ordre de {@code columns}), {@code null} si aucune moyenne calculable.
     * @param generalAverage Moyenne générale = somme(moyenne_matière × coef) / somme(coef) sur les matières avec une moyenne.
     */
    public record PeriodNotesGridRow(
        long studentId,
        String lastName,
        String firstName,
        List<Double> averages,
        Double generalAverage
    ) {
    }

    /**
     * @param snapshotAsOf      Horodatage du lot de snapshots (même valeur pour toutes les lignes d’une batch) ; null si calcul live.
     * @param dataFromSnapshot  true si issu de {@code student_grading_snapshots}.
     */
    public record PeriodNotesGridResponse(
        long classId,
        long gradingPeriodId,
        String gradingPeriodName,
        double compositionWeight,
        List<ClassSubjectColumn> columns,
        List<PeriodNotesGridRow> rows,
        Instant snapshotAsOf,
        boolean dataFromSnapshot
    ) {
    }

    /**
     * Détail par matière pour un élève sur une période (tableau de bord bulletins).
     */
    public record StudentPeriodSubjectRow(
        long classSubjectId,
        String subjectCode,
        String subjectName,
        double coefficient,
        Double continuousAverage,
        Double compositionAverage,
        Double periodFinalAverage
    ) {
    }

    /**
     * @param generalAverage  Moyenne générale pondérée (null si aucune matière avec moyenne).
     * @param rankInClass     Rang (1 = premier) : nombre d’élèves strictement devant, +1.
     * @param classSize       Effectif de la classe.
     * @param evaluatedEvaluationsCount Nombre d’évaluations distinctes avec au moins une note saisie pour l’élève.
     * @param snapshotAsOf              Dernier recalcul stocké en base ; null si {@code dataFromSnapshot} est false.
     * @param dataFromSnapshot          true si issu d’un snapshot batch.
     */
    public record StudentPeriodBuildData(
        List<StudentPeriodSubjectRow> subjects,
        Double generalAverage,
        int evaluatedEvaluationsCount
    ) {
    }

    public record StudentPeriodDashboardResponse(
        long studentId,
        long gradingPeriodId,
        String gradingPeriodName,
        long classId,
        double compositionWeight,
        Double generalAverage,
        Integer rankInClass,
        int classSize,
        int evaluatedEvaluationsCount,
        List<StudentPeriodSubjectRow> subjects,
        Instant snapshotAsOf,
        boolean dataFromSnapshot
    ) {
    }
}

package friasoft.gn.schoolapp.service.finance;

import friasoft.gn.schoolapp.entity.school.FeeStructure;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;

/**
 * Dûs de scolarité Oct→Juin (9 mois) à partir du barème.
 * <ul>
 *   <li>Mode mensuel ({@code annual_tuition_fee} null) : 9 × mensualité.</li>
 *   <li>Mode annuel : 1er mois = reste après arrondi des autres au millier inférieur
 *       (ex. 1 675 000 → 187 000 puis 8 × 186 000).</li>
 *   <li>{@code tuition_payable_percent} : 100 = barème, 0 = exempté ; réaffectation via {@link #fromAnnual}.</li>
 * </ul>
 */
public final class TuitionMonthDues {

    public static final int MONTH_COUNT = 9;

    private TuitionMonthDues() {}

    /**
     * Premier mois de scolarité (octobre) de l’année scolaire : octobre de l’année civile
     * de {@code schoolYearStart} (ex. démarrage sept. 2026 → oct. 2026).
     */
    public static YearMonth firstTuitionMonth(LocalDate schoolYearStart) {
        if (schoolYearStart == null) {
            return YearMonth.now().withMonth(Month.OCTOBER.getValue());
        }
        return YearMonth.of(schoolYearStart.getYear(), Month.OCTOBER);
    }

    /**
     * Nombre de mois Oct→Juin déjà échus à la date {@code asOf} (0 à {@link #MONTH_COUNT}).
     */
    public static int dueMonthCountAsOf(LocalDate schoolYearStart, LocalDate asOf) {
        if (asOf == null) {
            asOf = LocalDate.now();
        }
        YearMonth first = firstTuitionMonth(schoolYearStart);
        YearMonth current = YearMonth.from(asOf);
        if (current.isBefore(first)) {
            return 0;
        }
        long months = first.until(current, java.time.temporal.ChronoUnit.MONTHS) + 1;
        return (int) Math.max(0, Math.min(MONTH_COUNT, months));
    }

    /**
     * Somme des dus des mois déjà échus (index 0 = octobre …).
     */
    public static double sumDueAsOf(double[] dues, LocalDate schoolYearStart, LocalDate asOf) {
        int n = dueMonthCountAsOf(schoolYearStart, asOf);
        if (dues == null || n <= 0) {
            return 0d;
        }
        double s = 0d;
        for (int i = 0; i < n && i < dues.length; i++) {
            s += Math.max(0d, dues[i]);
        }
        return s;
    }

    /**
     * Montants dus par mois (index 0 = octobre … 8 = juin), barème sans réduction.
     */
    public static double[] forFeeStructure(FeeStructure feeStructure) {
        return forFeeStructure(feeStructure, 100d);
    }

    /**
     * Montants dus après application du % à payer (0–100).
     */
    public static double[] forFeeStructure(FeeStructure feeStructure, double payablePercent) {
        if (feeStructure == null) {
            return zeros();
        }
        double[] base;
        if (feeStructure.getAnnualTuitionFee() != null) {
            base = fromAnnual(Math.max(0d, feeStructure.getAnnualTuitionFee()));
        } else {
            double monthly = Math.max(0d, nvl(feeStructure.getMonthlyTuitionFee()));
            base = new double[MONTH_COUNT];
            for (int i = 0; i < MONTH_COUNT; i++) {
                base[i] = monthly;
            }
        }
        return applyPayablePercent(base, payablePercent);
    }

    public static double totalExpected(FeeStructure feeStructure) {
        return totalExpected(feeStructure, 100d);
    }

    public static double totalExpected(FeeStructure feeStructure, double payablePercent) {
        return sum(forFeeStructure(feeStructure, payablePercent));
    }

    /**
     * Applique le % à payer sur des dus de référence.
     * À 100 % : dus inchangés. Sinon : total arrondi × % / 100, redistribué comme un annuel.
     */
    public static double[] applyPayablePercent(double[] baseDues, double payablePercent) {
        double p = clampPercent(payablePercent);
        if (baseDues == null || baseDues.length == 0) {
            return zeros();
        }
        if (p >= 100d - 1e-9) {
            double[] copy = new double[MONTH_COUNT];
            for (int i = 0; i < MONTH_COUNT; i++) {
                copy[i] = i < baseDues.length ? Math.max(0d, baseDues[i]) : 0d;
            }
            return copy;
        }
        if (p <= 1e-9) {
            return zeros();
        }
        long catalog = Math.round(sum(baseDues));
        long payable = Math.round(catalog * p / 100.0);
        return fromAnnual(payable);
    }

    public static double clampPercent(double raw) {
        if (Double.isNaN(raw) || Double.isInfinite(raw)) {
            return 100d;
        }
        return Math.max(0d, Math.min(100d, raw));
    }

    public static double sum(double[] dues) {
        if (dues == null) {
            return 0d;
        }
        double s = 0d;
        for (double d : dues) {
            s += Math.max(0d, d);
        }
        return s;
    }

    /**
     * Répartition annuelle : autres mois au millier inférieur de (annuel / 9) ;
     * le 1er mois absorbe le reste (souvent au millier supérieur).
     */
    public static double[] fromAnnual(double annualRaw) {
        long annual = Math.max(0L, Math.round(annualRaw));
        double[] dues = new double[MONTH_COUNT];
        if (annual == 0L) {
            return dues;
        }
        long average = annual / MONTH_COUNT;
        long otherMonths = (average / 1000L) * 1000L;
        long firstMonth = annual - otherMonths * (MONTH_COUNT - 1L);
        dues[0] = firstMonth;
        for (int i = 1; i < MONTH_COUNT; i++) {
            dues[i] = otherMonths;
        }
        return dues;
    }

    private static double[] zeros() {
        return new double[MONTH_COUNT];
    }

    private static double nvl(Double value) {
        return value == null ? 0d : value;
    }
}

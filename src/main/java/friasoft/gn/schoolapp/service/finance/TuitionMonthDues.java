package friasoft.gn.schoolapp.service.finance;

import friasoft.gn.schoolapp.entity.school.FeeStructure;

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

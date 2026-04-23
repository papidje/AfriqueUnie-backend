package friasoft.gn.schoolapp.service.document;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;

import java.util.Locale;

/**
 * Montant en lettres pour reçus (francs guinéens, entiers).
 * Utilise ICU pour un français correct (ex. soixante-quatorze millions…).
 */
public final class GnfAmountInWords {

    private static final RuleBasedNumberFormat SPELL =
        new RuleBasedNumberFormat(ULocale.FRENCH, RuleBasedNumberFormat.SPELLOUT);

    private GnfAmountInWords() {
    }

    public static String format(double amountGnf) {
        long n = Math.round(amountGnf);
        if (n < 0) {
            return "Moins " + format(-n).toLowerCase(Locale.FRANCE);
        }
        if (n == 0) {
            return "Zéro franc guinéen";
        }
        String spelled = SPELL.format(n);
        if (Character.isLowerCase(spelled.charAt(0))) {
            spelled = Character.toUpperCase(spelled.charAt(0)) + spelled.substring(1);
        }
        if (n == 1L) {
            return spelled + " franc guinéen";
        }
        return spelled + " francs guinéens";
    }
}

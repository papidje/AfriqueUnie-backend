package friasoft.gn.schoolapp.util;

import java.util.regex.Pattern;

public final class GuineaContactValidation {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private GuineaContactValidation() {}

    /** Formats acceptés : +224 628 11 22 33, 00224 628 11 22 33, 528 11 22 33 */
    public static boolean isValidGuineaPhone(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        String compact = compactPhone(raw);
        return compact.matches("^\\+224[0-9]{9}$")
            || compact.matches("^00224[0-9]{9}$")
            || compact.matches("^[0-9]{9}$");
    }

    public static String compactPhone(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", "");
    }

    public static boolean isValidEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        return EMAIL.matcher(raw.trim()).matches();
    }

    public static void requireValidGuineaPhone(String raw, String fieldLabel) {
        if (!isValidGuineaPhone(raw)) {
            throw new IllegalArgumentException(
                fieldLabel + " invalide. Formats : +224 628 11 22 33, 00224 628 11 22 33 ou 528 11 22 33."
            );
        }
    }

    public static void requireValidEmail(String raw, String fieldLabel) {
        if (!isValidEmail(raw)) {
            throw new IllegalArgumentException(fieldLabel + " invalide.");
        }
    }
}

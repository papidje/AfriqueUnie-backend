package friasoft.gn.schoolapp.util;

import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.entity.school.SchoolClass;

import java.util.Comparator;

/**
 * Ordre pédagogique d’affichage des groupes / niveaux.
 * Priorité à {@code sortOrder} (administrable) ; repli sur le code alphabétique.
 */
public final class ClassLevelOrdering {

    private ClassLevelOrdering() {}

    public static int groupSortKey(ClassLevelGroup group) {
        if (group == null) {
            return Integer.MAX_VALUE;
        }
        return group.getSortOrder();
    }

    public static int groupSortKey(String groupCode) {
        // Conservé pour les appels qui n’ont que le code (ex. options matières).
        if (groupCode == null || groupCode.isBlank()) {
            return Integer.MAX_VALUE;
        }
        return switch (groupCode.trim().toUpperCase()) {
            case "PRE" -> 1;
            case "MAT" -> 2;
            case "PRI" -> 3;
            case "COL" -> 4;
            case "LYC" -> 5;
            default -> Integer.MAX_VALUE;
        };
    }

    public static int levelSortKey(ClassLevel level) {
        if (level == null) {
            return Integer.MAX_VALUE;
        }
        return level.getSortOrder();
    }

    public static int levelSortKey(String levelCode) {
        if (levelCode == null || levelCode.isBlank()) {
            return Integer.MAX_VALUE;
        }
        return switch (levelCode.trim().toUpperCase()) {
            case "GAR" -> 10;
            case "PS" -> 20;
            case "MS" -> 30;
            case "GS" -> 40;
            case "CP1" -> 50;
            case "CP2" -> 60;
            case "CE1" -> 70;
            case "CE2" -> 80;
            case "CM1" -> 90;
            case "CM2" -> 100;
            case "7E" -> 110;
            case "8E" -> 120;
            case "9E" -> 130;
            case "10E" -> 140;
            case "11E" -> 150;
            case "12E" -> 160;
            case "TLE" -> 170;
            default -> Integer.MAX_VALUE;
        };
    }

    public static Comparator<ClassLevel> classLevelComparator() {
        return Comparator
            .comparingInt((ClassLevel lv) -> groupSortKey(lv.getGroup()))
            .thenComparingInt(ClassLevelOrdering::levelSortKey)
            .thenComparing(lv -> lv.getCode() != null ? lv.getCode() : "", String::compareToIgnoreCase);
    }

    public static Comparator<ClassLevelGroup> classLevelGroupComparator() {
        return Comparator
            .comparingInt((ClassLevelGroup g) -> groupSortKey(g))
            .thenComparing(g -> g.getCode() != null ? g.getCode() : "", String::compareToIgnoreCase);
    }

    public static Comparator<SchoolClass> schoolClassComparator() {
        return Comparator
            .comparing((SchoolClass sc) -> sc.getLevel(), Comparator.nullsLast(classLevelComparator()))
            .thenComparing(sc -> sc.getName() != null ? sc.getName() : "", String::compareToIgnoreCase)
            .thenComparing(sc -> sc.getId() != null ? sc.getId() : 0L);
    }
}

package friasoft.gn.schoolapp.util;

import friasoft.gn.schoolapp.entity.school.ClassLevel;
import friasoft.gn.schoolapp.entity.school.ClassLevelGroup;
import friasoft.gn.schoolapp.entity.school.SchoolClass;

import java.util.Comparator;
import java.util.Map;

/**
 * Ordre pédagogique d’affichage des groupes / niveaux (indépendant des IDs d’insertion).
 */
public final class ClassLevelOrdering {

    private static final Map<String, Integer> GROUP_ORDER = Map.of(
        "PRE", 1,
        "MAT", 2,
        "PRI", 3,
        "COL", 4,
        "LYC", 5
    );

    private static final Map<String, Integer> LEVEL_ORDER = Map.ofEntries(
        Map.entry("GAR", 10),
        Map.entry("PS", 20),
        Map.entry("MS", 30),
        Map.entry("GS", 40),
        Map.entry("CP1", 50),
        Map.entry("CP2", 60),
        Map.entry("CE1", 70),
        Map.entry("CE2", 80),
        Map.entry("CM1", 90),
        Map.entry("CM2", 100),
        Map.entry("7E", 110),
        Map.entry("8E", 120),
        Map.entry("9E", 130),
        Map.entry("10E", 140),
        Map.entry("11E", 150),
        Map.entry("12E", 160),
        Map.entry("TLE", 170)
    );

    private ClassLevelOrdering() {}

    public static int groupSortKey(String groupCode) {
        if (groupCode == null || groupCode.isBlank()) {
            return Integer.MAX_VALUE;
        }
        return GROUP_ORDER.getOrDefault(groupCode, Integer.MAX_VALUE);
    }

    public static int levelSortKey(String levelCode) {
        if (levelCode == null || levelCode.isBlank()) {
            return Integer.MAX_VALUE;
        }
        return LEVEL_ORDER.getOrDefault(levelCode, Integer.MAX_VALUE);
    }

    public static Comparator<ClassLevel> classLevelComparator() {
        return Comparator
            .comparingInt((ClassLevel lv) -> {
                ClassLevelGroup g = lv.getGroup();
                return groupSortKey(g != null ? g.getCode() : null);
            })
            .thenComparingInt(lv -> levelSortKey(lv.getCode()))
            .thenComparing(lv -> lv.getCode() != null ? lv.getCode() : "", String::compareToIgnoreCase);
    }

    public static Comparator<SchoolClass> schoolClassComparator() {
        return Comparator
            .comparing((SchoolClass sc) -> sc.getLevel(), Comparator.nullsLast(classLevelComparator()))
            .thenComparing(sc -> sc.getName() != null ? sc.getName() : "", String::compareToIgnoreCase)
            .thenComparing(sc -> sc.getId() != null ? sc.getId() : 0L);
    }
}

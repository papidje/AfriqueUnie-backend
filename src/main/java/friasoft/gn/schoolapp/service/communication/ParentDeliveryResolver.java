package friasoft.gn.schoolapp.service.communication;

import friasoft.gn.schoolapp.entity.school.Parent;
import friasoft.gn.schoolapp.entity.school.Student;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
public class ParentDeliveryResolver {

    public record ParentDeliveryTarget(long parentDedupeId, String email, String phone, String greetingKey) {
    }

    /**
     * Une ligne par adresse e-mail distincte (fusion père/mère/tuteur comme pour les reçus).
     */
    public List<ParentDeliveryTarget> resolveDistinctEmailTargets(Student student) {
        if (student == null) {
            return List.of();
        }
        Map<String, RecipientAgg> byNormEmail = new LinkedHashMap<>();
        mergeParent(byNormEmail, student.getFather());
        mergeParent(byNormEmail, student.getMother());
        mergeParent(byNormEmail, student.getTutor());

        List<ParentDeliveryTarget> out = new ArrayList<>();
        for (RecipientAgg agg : byNormEmail.values()) {
            if (!StringUtils.hasText(agg.email())) {
                continue;
            }
            out.add(
                new ParentDeliveryTarget(
                    agg.primaryParentId(),
                    agg.email().trim(),
                    Optional.ofNullable(agg.phone()).filter(StringUtils::hasText).map(String::trim).orElse(""),
                    agg.buildGreeting()
                )
            );
        }
        return out;
    }

    private static void mergeParent(Map<String, RecipientAgg> byKey, @Nullable Parent p) {
        if (p == null || !StringUtils.hasText(p.getEmail())) {
            return;
        }
        String email = p.getEmail().trim();
        if (!isPlausibleEmail(email)) {
            return;
        }
        String norm = normalize(email);
        RecipientAgg agg = byKey.computeIfAbsent(norm, k -> new RecipientAgg(email, p.getPhone()));
        agg.addParentId(p.getId());
        agg.addName(displayName(p));
    }

    private static String displayName(Parent p) {
        return (p.getFirstName() + " " + p.getLastName()).trim();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isPlausibleEmail(String email) {
        return email != null && email.contains("@") && email.length() >= 5;
    }

    private static final class RecipientAgg {
        private final String email;
        private final String phoneFallback;
        private final Set<Long> parentIds = new TreeSet<>();
        private final List<String> names = new ArrayList<>();

        RecipientAgg(String email, String phoneFallback) {
            this.email = email;
            this.phoneFallback = phoneFallback;
        }

        void addParentId(Long id) {
            if (id != null) {
                parentIds.add(id);
            }
        }

        void addName(String name) {
            if (name != null && !name.isBlank()) {
                names.add(name.trim());
            }
        }

        String email() {
            return email;
        }

        String phone() {
            return phoneFallback;
        }

        long primaryParentId() {
            if (!parentIds.isEmpty()) {
                return parentIds.iterator().next();
            }
            return 0L;
        }

        String buildGreeting() {
            if (names.isEmpty()) {
                return "Bonjour,";
            }
            return "Bonjour " + String.join(" et ", names) + ",";
        }
    }
}

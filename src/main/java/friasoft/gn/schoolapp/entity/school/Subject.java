package friasoft.gn.schoolapp.entity.school;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subjects")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, allowGetters = true)
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    /**
     * {@code null} = matière du référentiel partagé (toutes écoles du tenant) ;
     * sinon matière propre à l’établissement.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    /**
     * Groupes de cycle où la matière peut être affectée à une classe.
     * Vide = non assignable.
     */
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "subject_level_groups",
        joinColumns = @JoinColumn(name = "subject_id"),
        inverseJoinColumns = @JoinColumn(name = "class_level_group_id")
    )
    private Set<ClassLevelGroup> levelGroups = new HashSet<>();

    @JsonProperty("schoolId")
    public Long getSchoolId() {
        return school == null ? null : school.getId();
    }

    @JsonProperty("levelGroupCodes")
    public List<String> getLevelGroupCodes() {
        if (levelGroups == null || levelGroups.isEmpty()) {
            return List.of();
        }
        return levelGroups.stream()
            .map(ClassLevelGroup::getCode)
            .filter(c -> c != null && !c.isBlank())
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    public void replaceLevelGroups(Set<ClassLevelGroup> groups) {
        if (levelGroups == null) {
            levelGroups = new HashSet<>();
        } else {
            levelGroups.clear();
        }
        if (groups != null) {
            levelGroups.addAll(groups);
        }
    }
}

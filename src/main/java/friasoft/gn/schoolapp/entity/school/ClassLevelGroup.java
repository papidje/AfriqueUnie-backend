package friasoft.gn.schoolapp.entity.school;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "class_level_groups")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassLevelGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    @ToString.Include
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    /** Ordre d’affichage pédagogique (administrable). */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 100;

    @JsonIgnore
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<ClassLevel> levels = new ArrayList<>();
}

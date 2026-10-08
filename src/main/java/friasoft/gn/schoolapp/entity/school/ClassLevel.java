package friasoft.gn.schoolapp.entity.school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "class_levels")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    /** Ordre d’affichage pédagogique (administrable). */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 1000;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler", "levels"}, allowSetters = true)
    private ClassLevelGroup group;
}


package friasoft.gn.schoolapp.entity.school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "class_level_groups")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassLevelGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<ClassLevel> levels = new ArrayList<>();
}

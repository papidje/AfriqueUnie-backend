package friasoft.gn.schoolapp.entity.school;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @JsonProperty("schoolId")
    public Long getSchoolId() {
        return school == null ? null : school.getId();
    }
}

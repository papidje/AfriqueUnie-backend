package friasoft.gn.schoolapp.entity;
import friasoft.gn.schoolapp.enums.CivilityEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity
@Table(name = "responsibles")
public class Responsible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private CivilityEnum civility;
    private String lastName;
    private String firstName;
    private String adress;
    @JoinColumn(name = "contact_id")
    @OneToOne
    private Contact contact;

}

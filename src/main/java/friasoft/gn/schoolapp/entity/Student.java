package friasoft.gn.schoolapp.entity;

import friasoft.gn.schoolapp.enums.CivilityEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;
import java.util.Random;

@Data
@AllArgsConstructor
@Entity
@Table(name = "students")
public class Student implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String matricule;

    @Enumerated(EnumType.STRING)
    private CivilityEnum civility;

    private String lastName;

    private String firstName;

    private Date birthDate;

    private String adress;
    
    @ManyToMany(fetch = FetchType.LAZY)
    List<Responsible> responsibles;

    public String buildMatricule() {
        Random random = new Random();
        return new StringBuilder()
            .append(civility.getId())
            .append(birthDate.getYear())
            .append(birthDate.getMonth())
            .append(random.nextInt(999999))
            .toString();
    }
}

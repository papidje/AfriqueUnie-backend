package friasoft.gn.schoolapp.entity;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;
import java.util.Random;

import friasoft.gn.schoolapp.enums.CivilityEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity
@Table(name = "student")
public class Student implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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

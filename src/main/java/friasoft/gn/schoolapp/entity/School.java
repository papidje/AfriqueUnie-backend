package friasoft.gn.schoolapp.entity;

import java.sql.Date;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor 
@Entity
@Table(name = "school")
public class School {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Short id;
    private String name;
    private String adress;
    private Date openDate;
    private String logo;
}

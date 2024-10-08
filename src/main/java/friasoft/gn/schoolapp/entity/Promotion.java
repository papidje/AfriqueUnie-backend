package friasoft.gn.schoolapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity
@Table(name = "promotion")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Short id;

    private String name;

    @ManyToOne
    private Grade grade;
}

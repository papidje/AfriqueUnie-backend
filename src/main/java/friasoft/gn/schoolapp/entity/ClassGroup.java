package friasoft.gn.schoolapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity
@Table(name = "class_group")
public class ClassGroup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String name;
}

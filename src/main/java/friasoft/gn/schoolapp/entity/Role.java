package friasoft.gn.schoolapp.entity;

import friasoft.gn.schoolapp.enums.RoleEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Short id;
    @Enumerated(EnumType.STRING)
    private RoleEnum name; 
}
















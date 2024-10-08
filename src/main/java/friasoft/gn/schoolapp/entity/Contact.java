package friasoft.gn.schoolapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity
@Table(name = "contact")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Short id;

    private String primePhone;

    private String backupPhone;

    private String primeMail;
    
    private String backupMail;
}

package friasoft.gn.schoolapp.entity;

import java.io.Serializable;
import java.sql.Date;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "activation")
public class Activation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Short id;

    @JoinColumn(name = "user_id")
    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "school_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private School school;

    @Column(name = "registration_date")
    private Date registrationDate;

    @Column(name = "code")
    private String code;
}

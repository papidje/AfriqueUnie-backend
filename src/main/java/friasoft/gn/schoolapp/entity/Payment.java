package friasoft.gn.schoolapp.entity;

import friasoft.gn.schoolapp.enums.PaymentEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Enumerated(EnumType.STRING)
    private PaymentEnum type;

    private double amount;

    private double restToPay;
}

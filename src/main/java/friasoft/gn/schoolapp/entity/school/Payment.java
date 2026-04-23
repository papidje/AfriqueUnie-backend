package friasoft.gn.schoolapp.entity.school;

import friasoft.gn.schoolapp.entity.auth.User;
import friasoft.gn.schoolapp.tenancy.TenantAware;
import friasoft.gn.schoolapp.tenancy.TenantHibernateFilterAspect;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "student_payments",
    indexes = {
        @Index(name = "idx_student_payments_account_id", columnList = "student_account_id")
    }
)
@Filter(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    condition = "tenant_id = :" + TenantHibernateFilterAspect.TENANT_FILTER_PARAM
)
public class Payment implements TenantAware {

    public enum PaymentType {
        INSCRIPTION,
        REINSCRIPTION,
        SCOLARITE,
        /** Frais de fournitures (montant aligné sur la structure de frais). */
        FOURNITURES
    }

    public enum PaymentMode {
        ESPECES,
        ORANGE_MONEY,
        MOOV_MONEY,
        VIREMENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_account_id", nullable = false)
    private StudentAccount studentAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentType paymentType;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "GNF";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 30)
    private PaymentMode paymentMode;

    @CreationTimestamp
    private LocalDateTime paymentDate;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Même référence pour toutes les lignes créées lors d’un même encaissement (reçu / duplicata / regroupement historique). */
    @Column(name = "receipt_reference", length = 64)
    private String receiptReference;

    /** Personne ayant enregistré l’encaissement (saisie libre). */
    @Column(name = "recorded_by", length = 200)
    private String recordedBy;

    /** Pour {@link PaymentType#SCOLARITE} : code mois (ex. OCT, NOV, …). Null pour les autres types ou données historiques. */
    @Column(name = "tuition_month_code", length = 16)
    private String tuitionMonthCode;

    /** Utilisateur connecté au moment de l’enregistrement (historique fiche élève, hors reçu). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_user_id")
    private User validatedBy;
}


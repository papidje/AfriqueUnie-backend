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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "students")
@Filter(
    name = TenantHibernateFilterAspect.TENANT_FILTER_NAME,
    condition = "tenant_id = :" + TenantHibernateFilterAspect.TENANT_FILTER_PARAM
)
public class Student implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Civility civility;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "father_id")
    private Parent father;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mother_id")
    private Parent mother;

    private LocalDate birthDate;

    @Column(length = 150)
    private String birthPlace;

    @Column(length = 120)
    private String nationality;

    private String matricule;

    /** Numéro de carte scolaire (nouvelle à l’inscription ; modifiable en cas de perte). */
    @Column(name = "card_number", length = 50)
    private String cardNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @Column(length = 255)
    private String address;

    @Column(length = 40)
    private String communicationPhone;

    @Column(length = 180)
    private String communicationEmail;

    @Column(length = 150)
    private String emergencyContactName;

    @Column(length = 40)
    private String emergencyContactPhone;

    @Column(length = 20)
    private String bloodGroup;

    @Column(length = 500)
    private String allergies;

    @Column(length = 150)
    private String tutorName;

    @Column(length = 150)
    private String tutorProfession;

    @Column(length = 40)
    private String tutorPhone;

    @Column(length = 180)
    private String tutorEmail;

    @Column(length = 255)
    private String photoPath;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EnrollmentStatus enrollmentStatus = EnrollmentStatus.INSCRIT;

    @Column(length = 255)
    private String classHistory;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    public enum Civility {
        MONSIEUR, MADAME
    }

    public enum EnrollmentStatus {
        INSCRIT,
        /** Élève détaché d’une classe (ex. scission) en attente de réaffectation — reste dans l’école. */
        SANS_CLASSE,
        /** Élève ayant quitté l’établissement (désinscrit). */
        DESINSCRIT,
        TRANSFERE
    }

    /**
     * Format : {@code [1|2]}{@code AAAA}{@code MM}{@code RRRR}
     * (civilité, année, mois sur 2 chiffres, aléatoire sur 4 chiffres).
     */
    public String buildMatricule() {
        if (birthDate == null) {
            throw new IllegalStateException("Date de naissance obligatoire pour générer le matricule.");
        }
        int suffix = ThreadLocalRandom.current().nextInt(10_000);
        return String.format(
            "%s%04d%02d%04d",
            civility == Civility.MONSIEUR ? "1" : "2",
            birthDate.getYear(),
            birthDate.getMonthValue(),
            suffix
        );
    }
}

package friasoft.gn.schoolapp.entity.school;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Date;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor 
@Entity
@Table(name = "schools")
public class School {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    private String name;
    private String adress;
    private String contact;
    private Date openDate;
    private String logo;

    /** Clé thème front (ex. classique, emeraude, bordeaux) — white label. */
    @Column(name = "theme_name", length = 64)
    private String themeName = "classique";

    /** Clé police (ex. inter, montserrat) — white label. */
    @Column(name = "font_name", length = 64)
    private String fontName = "inter";

    private boolean isActive = false;
    private Instant created_at;
    private Instant updated_at;
}

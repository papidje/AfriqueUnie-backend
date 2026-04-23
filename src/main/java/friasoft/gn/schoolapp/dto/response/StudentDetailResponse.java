package friasoft.gn.schoolapp.dto.response;

import friasoft.gn.schoolapp.dto.ParentDtos.ParentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDetailResponse {
    private Long id;
    private String civility;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String birthPlace;
    private String nationality;
    private String matricule;
    private String address;
    private String communicationPhone;
    private String communicationEmail;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String bloodGroup;
    private String allergies;
    private String tutorName;
    private String tutorProfession;
    private String tutorPhone;
    private String tutorEmail;
    private String photoPath;
    private String enrollmentStatus;
    private String classHistory;
    private String schoolClassName;
    private String schoolYearLabel;
    private ParentResponse father;
    private ParentResponse mother;
}

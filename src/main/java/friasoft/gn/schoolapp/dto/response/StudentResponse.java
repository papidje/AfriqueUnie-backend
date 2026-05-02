package friasoft.gn.schoolapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponse {
    private Long id;
    private String civility;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String matricule;
}

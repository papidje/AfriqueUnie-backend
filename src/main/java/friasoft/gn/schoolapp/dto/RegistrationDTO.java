package friasoft.gn.schoolapp.dto;

public record RegistrationDTO(
    StudentRegistrationDTO student,
    ParentRegistrationDTO father,
    ParentRegistrationDTO mother,
    Long classId,
    Double amountPaid,
    String currency
) {
}


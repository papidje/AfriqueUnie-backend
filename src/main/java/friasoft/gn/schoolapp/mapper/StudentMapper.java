package friasoft.gn.schoolapp.mapper;

import friasoft.gn.schoolapp.dto.ParentDtos.ParentResponse;
import friasoft.gn.schoolapp.dto.response.StudentDetailResponse;
import friasoft.gn.schoolapp.dto.response.StudentResponse;
import friasoft.gn.schoolapp.entity.school.Parent;
import friasoft.gn.schoolapp.entity.school.Student;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StudentMapper {
    public StudentResponse toDto(Student student) {
        StudentResponse studentResponse = new StudentResponse();
        studentResponse.setId(student.getId());
        studentResponse.setFirstName(student.getFirstName());
        studentResponse.setLastName(student.getLastName());
        studentResponse.setBirthDate(student.getBirthDate());
        if (student.getCivility() != null) {
            studentResponse.setCivility(student.getCivility().name());
        }
        studentResponse.setMatricule(student.getMatricule());
        return studentResponse;
    }

    public StudentDetailResponse toDetailDto(Student student) {
        StudentDetailResponse.StudentDetailResponseBuilder b = StudentDetailResponse.builder()
            .id(student.getId())
            .firstName(student.getFirstName())
            .lastName(student.getLastName())
            .birthDate(student.getBirthDate())
            .birthPlace(student.getBirthPlace())
            .nationality(student.getNationality())
            .matricule(student.getMatricule())
            .address(student.getAddress())
            .communicationPhone(student.getCommunicationPhone())
            .communicationEmail(student.getCommunicationEmail())
            .emergencyContactName(student.getEmergencyContactName())
            .emergencyContactPhone(student.getEmergencyContactPhone())
            .bloodGroup(student.getBloodGroup())
            .allergies(student.getAllergies())
            .tutorName(student.getTutorName())
            .tutorProfession(student.getTutorProfession())
            .tutorPhone(student.getTutorPhone())
            .tutorEmail(student.getTutorEmail())
            .photoPath(student.getPhotoPath())
            .classHistory(student.getClassHistory())
            .schoolClassName(student.getSchoolClass() != null ? student.getSchoolClass().getName() : null)
            .schoolYearLabel(
                student.getSchoolClass() != null && student.getSchoolClass().getYear() != null
                    ? student.getSchoolClass().getYear().getLabel()
                    : null
            );
        if (student.getCivility() != null) {
            b.civility(student.getCivility().name());
        }
        if (student.getEnrollmentStatus() != null) {
            b.enrollmentStatus(student.getEnrollmentStatus().name());
        }
        if (student.getFather() != null) {
            b.father(toParentResponse(student.getFather()));
        }
        if (student.getMother() != null) {
            b.mother(toParentResponse(student.getMother()));
        }
        return b.build();
    }

    private static ParentResponse toParentResponse(Parent p) {
        return new ParentResponse(
            p.getId(),
            p.getTenantId(),
            p.getLastName(),
            p.getFirstName(),
            p.getPhone(),
            p.getEmail(),
            p.getProfession(),
            p.getAddress()
        );
    }

    public Student toEntity(StudentResponse dto) {
        Student student = new Student();
        student.setId(dto.getId());
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setBirthDate(dto.getBirthDate());
        if (dto.getCivility() != null && !dto.getCivility().isBlank()) {
            student.setCivility(Student.Civility.valueOf(dto.getCivility().trim().toUpperCase()));
        }
        return student;
    }

    public List<StudentResponse> toDtoList(List<Student> students) {
        return students.stream().map(this::toDto).collect(Collectors.toList());
    }
}

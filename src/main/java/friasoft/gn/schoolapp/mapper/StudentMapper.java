package friasoft.gn.schoolapp.mapper;

import friasoft.gn.schoolapp.dto.response.StudentResponse;
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

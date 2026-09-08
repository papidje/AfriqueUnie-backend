package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.request.StudentPatchRequest;
import friasoft.gn.schoolapp.dto.request.StudentProfileUpdateRequest;
import friasoft.gn.schoolapp.entity.school.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IStudentService {
    Page<Student> findAll(Pageable pageable);
    Optional<Student> findById(Long id);
    List<Student> searchByLastName(String lastName);
    List<Student> findByClass(Long classId);
    List<Student> findUnassignedBySchool(Long schoolId);
    Student save(Student student);
    void delete(Long id);

    Student updateProfile(Long id, StudentProfileUpdateRequest request);
    Student patchStudent(Long id, StudentPatchRequest request);
    Student updatePhotoPath(Long id, String photoPath);

    Student transferToClass(Long studentId, Long targetClassId);
    Student unassignFromClass(Long studentId);
    Student unenroll(Long studentId);

    void unlinkFather(Long studentId);

    void unlinkMother(Long studentId);
}

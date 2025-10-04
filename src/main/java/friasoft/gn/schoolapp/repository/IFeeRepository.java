package friasoft.gn.schoolapp.repository;

import friasoft.gn.schoolapp.entity.school.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// FeeRepository.java
@Repository
public interface IFeeRepository extends JpaRepository<Fee, Long> {
    List<Fee> findByClassRef_Id(Long classId);
    List<Fee> findByName(Fee.FeeType type);
}

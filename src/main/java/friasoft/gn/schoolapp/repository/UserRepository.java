package friasoft.gn.schoolapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import friasoft.gn.schoolapp.entity.User;

public interface UserRepository extends JpaRepository<User, Short>{

    Optional<User> findByEmail(String email);
}

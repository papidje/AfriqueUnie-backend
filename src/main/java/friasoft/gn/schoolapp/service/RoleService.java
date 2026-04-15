package friasoft.gn.schoolapp.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import friasoft.gn.schoolapp.entity.auth.User;
import java.util.List;

@AllArgsConstructor
@Service
public class RoleService {
    public List<String> getAll() {
        return java.util.Arrays.stream(User.UserRole.values())
            .map(Enum::name)
            .toList();
    }
}

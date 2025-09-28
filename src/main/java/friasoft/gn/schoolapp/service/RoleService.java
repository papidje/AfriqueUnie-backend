package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.entity.auth.Role;
import friasoft.gn.schoolapp.repository.RoleRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class RoleService {

    private RoleRepository roleRepository;

    public List<Role> getAll() {
        return this.roleRepository.findAll();
    }
}

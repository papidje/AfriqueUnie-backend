package friasoft.gn.schoolapp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import friasoft.gn.schoolapp.entity.Role;
import friasoft.gn.schoolapp.repository.RoleRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class RoleService {

    private RoleRepository roleRepository;

    public List<Role> getAll() {
        return this.roleRepository.findAll();
    }
}

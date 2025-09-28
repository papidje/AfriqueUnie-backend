package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.auth.Role;
import friasoft.gn.schoolapp.service.RoleService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("roles")
public class RoleController {

    private RoleService roleService;

    @GetMapping
    public List<Role> getRoles() {
        return this.roleService.getAll();
    }
}

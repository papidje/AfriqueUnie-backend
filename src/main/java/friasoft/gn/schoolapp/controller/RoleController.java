package friasoft.gn.schoolapp.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import friasoft.gn.schoolapp.entity.Role;
import friasoft.gn.schoolapp.service.RoleService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


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

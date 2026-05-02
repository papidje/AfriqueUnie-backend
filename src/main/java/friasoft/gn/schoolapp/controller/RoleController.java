package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.service.RoleService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.READ;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("roles")
public class RoleController {

    private RoleService roleService;

    @PreAuthorize(READ)
    @GetMapping
    public List<String> getRoles() {
        return this.roleService.getAll();
    }
}

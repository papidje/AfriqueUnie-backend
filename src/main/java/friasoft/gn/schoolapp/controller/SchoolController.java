package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.School;
import friasoft.gn.schoolapp.service.SchoolService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("schools")
public class SchoolController {
    private final SchoolService schoolService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public void create(@RequestBody School school) {
        log.info("creation");
        this.schoolService.create(school);
    }

    @GetMapping
    public List<School> getSchools() {
        return this.schoolService.getAll();
    }

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }
    
}

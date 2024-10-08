package friasoft.gn.schoolapp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import friasoft.gn.schoolapp.entity.School;
import friasoft.gn.schoolapp.service.SchoolService;
import lombok.AllArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@AllArgsConstructor
@RestController
@RequestMapping("schools")
public class SchoolController {
    private final SchoolService schoolService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public void create(@RequestBody School school) {
        this.schoolService.create(school);
    }

    @GetMapping
    public List<School> getSchools() {
        return this.schoolService.getAll();
    }

    
    
}

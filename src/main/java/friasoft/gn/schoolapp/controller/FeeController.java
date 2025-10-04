package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.entity.school.Fee;
import friasoft.gn.schoolapp.service.FeeService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees")
@AllArgsConstructor
public class FeeController {

    private final FeeService service;

    @GetMapping("/{id}")
    public ResponseEntity<Fee> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/class/{classId}")
    public List<Fee> getByClass(@PathVariable Long classId) {
        return service.findByClass(classId);
    }

    @PostMapping
    public Fee create(@RequestBody Fee fee) {
        return service.save(fee);
    }
}

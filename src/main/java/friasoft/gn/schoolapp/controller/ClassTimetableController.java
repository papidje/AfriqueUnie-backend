package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.TimetableDtos.TimetableCellWriteDto;
import friasoft.gn.schoolapp.dto.TimetableDtos.TimetableViewDto;
import friasoft.gn.schoolapp.service.ClassTimetableService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@AllArgsConstructor
public class ClassTimetableController {

    private final ClassTimetableService timetableService;

    @GetMapping("/api/school-classes/{classId}/timetable")
    public TimetableViewDto get(@PathVariable Long classId) {
        try {
            return timetableService.getTimetable(classId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PutMapping("/api/school-classes/{classId}/timetable/cell")
    public TimetableViewDto setCell(@PathVariable Long classId, @RequestBody TimetableCellWriteDto body) {
        try {
            return timetableService.setCell(classId, body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}

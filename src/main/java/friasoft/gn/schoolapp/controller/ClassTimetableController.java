package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.TimetableDtos.TimetableCellWriteDto;
import friasoft.gn.schoolapp.dto.TimetableDtos.TimetableViewDto;
import friasoft.gn.schoolapp.service.ClassTimetableService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.READ;
import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.WRITE;

@RestController
@AllArgsConstructor
public class ClassTimetableController {

    private final ClassTimetableService timetableService;

    @PreAuthorize(READ)
    @GetMapping("/api/school-classes/{classId}/timetable")
    public TimetableViewDto get(@PathVariable Long classId) {
        try {
            return timetableService.getTimetable(classId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PreAuthorize(WRITE)
    @PutMapping("/api/school-classes/{classId}/timetable/cell")
    public TimetableViewDto setCell(@PathVariable Long classId, @RequestBody TimetableCellWriteDto body) {
        try {
            return timetableService.setCell(classId, body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}

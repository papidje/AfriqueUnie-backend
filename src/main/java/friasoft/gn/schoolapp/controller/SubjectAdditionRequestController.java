package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.AddCommentRequest;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.CommentResponse;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.CreateRequest;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.RequestDetail;
import friasoft.gn.schoolapp.dto.SubjectAdditionRequestDtos.RequestSummary;
import friasoft.gn.schoolapp.service.SubjectAdditionRequestService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.READ;
import static friasoft.gn.schoolapp.security.SchoolUiSecurityExpressions.WRITE;

@RestController
@RequestMapping("/api/subject-addition-requests")
@AllArgsConstructor
public class SubjectAdditionRequestController {

    private final SubjectAdditionRequestService service;

    @PreAuthorize(WRITE)
    @PostMapping
    public RequestDetail create(@RequestParam Long schoolId, @RequestBody CreateRequest body) {
        try {
            return service.create(schoolId, body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize(READ)
    @GetMapping
    public List<RequestSummary> listMine(@RequestParam(required = false, defaultValue = "ALL") String status) {
        try {
            return service.listMine(status);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PreAuthorize(READ)
    @GetMapping("/{id}")
    public RequestDetail get(@PathVariable Long id) {
        return service.getDetail(id);
    }

    @PreAuthorize(WRITE)
    @PostMapping("/{id}/comments")
    public CommentResponse comment(@PathVariable Long id, @RequestBody AddCommentRequest body) {
        try {
            return service.addComment(id, body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}

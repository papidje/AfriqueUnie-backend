package friasoft.gn.schoolapp.controller;

import friasoft.gn.schoolapp.dto.response.DashboardResponse;
import friasoft.gn.schoolapp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @PreAuthorize("hasAnyRole('ADMIN_ECOLE','STAFF','TEACHER','DIRECTOR','ACCOUNTANT')")
    @GetMapping("/summary")
    public DashboardResponse getSummary(
        Authentication authentication,
        @RequestParam(required = false) Long schoolId,
        @RequestParam(defaultValue = "false") boolean mock
    ) {
        if (mock) {
            return dashboardService.getMockSummary();
        }
        return dashboardService.getSummary(authentication, schoolId);
    }
}

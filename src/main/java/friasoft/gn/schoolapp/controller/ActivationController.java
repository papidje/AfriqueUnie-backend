package friasoft.gn.schoolapp.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import friasoft.gn.schoolapp.dto.ActivationResponse;
import friasoft.gn.schoolapp.mapper.ActivationMapper;
import friasoft.gn.schoolapp.service.ActivationService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("activations")
public class ActivationController {

    private ActivationService activationService;
    private ActivationMapper activationMapper;

    @GetMapping()
    public List<ActivationResponse> getAll() {
        return this.activationService.getAll()
            .stream()
            .map(activationMapper::toActivationResponse)
            .toList();
    }
    
    @GetMapping("bySchool/{schoolId}")
    public List<ActivationResponse> getBySchool(@PathVariable("schoolId") short schoolId) {
        return this.activationService.findBySchool(schoolId)
            .stream()
            .map(activationMapper::toActivationResponse)
            .toList();
    }
}

package friasoft.gn.schoolapp.mapper;

import org.springframework.stereotype.Component;

import friasoft.gn.schoolapp.dto.ActivationResponse;
import friasoft.gn.schoolapp.entity.Activation;

@Component
public class ActivationMapper {

    public ActivationResponse toActivationResponse(Activation activation) {
        return new ActivationResponse(
            activation.getUser().getName(), 
            activation.getUser().getEmail(), 
            activation.getUser().getAuthorities().toString(), 
            activation.getRegistrationDate(), 
            activation.getCode());
    }
}

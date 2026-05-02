package friasoft.gn.schoolapp.mapper;

import friasoft.gn.schoolapp.dto.ActivationResponse;
import friasoft.gn.schoolapp.entity.auth.Activation;
import org.springframework.stereotype.Component;

@Component
public class ActivationMapper {

    public ActivationResponse toActivationResponse(Activation activation) {
        return new ActivationResponse(
            activation.getUser().getUsername(),
            activation.getUser().getFullname(),
            activation.getUser().getEmail(),
            activation.getUser().getAuthorities().toString(), 
            activation.getRegistrationDate(), 
            activation.getCode());
    }
}

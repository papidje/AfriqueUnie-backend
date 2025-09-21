package friasoft.gn.schoolapp.service;

import friasoft.gn.schoolapp.dto.ActivationRequest;
import friasoft.gn.schoolapp.dto.UserRequest;
import friasoft.gn.schoolapp.entity.Activation;
import friasoft.gn.schoolapp.entity.Role;
import friasoft.gn.schoolapp.entity.School;
import friasoft.gn.schoolapp.entity.User;
import friasoft.gn.schoolapp.repository.IActivationRepository;
import friasoft.gn.schoolapp.repository.RoleRepository;
import friasoft.gn.schoolapp.repository.SchoolRepository;
import friasoft.gn.schoolapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.*;

@Slf4j
@AllArgsConstructor
@Service
public class UserService implements UserDetailsService{

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private SchoolRepository schoolRepository;
    private IActivationRepository iActivationRepository;
    private BCryptPasswordEncoder passwordEncoder;

    public void registery(UserRequest userInput) {
        if(!userInput.email().contains("@")) {
            throw new RuntimeException("Email invalide");
        }
        Optional<User> userOptional = this.userRepository.findByEmail(userInput.email());
        if(userOptional.isPresent()) {
            throw new RuntimeException("Email deja utilisé");
        }

        Role role = roleRepository.findById(userInput.roleId()).orElseThrow();
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        School school = schoolRepository.findById(userInput.schoolId()).orElseThrow();
        User user = new User();
        user.setName(userInput.name());
        user.setEmail(userInput.email());
        user.setUsername(userInput.username());
        user.setRoles(roles);
        user.setSchool(school);
        user.setPassword(this.passwordEncoder.encode(userInput.password()));
        user = this.userRepository.save(user);

        Random random = new Random();
        Activation activation = new Activation();
        activation.setCode(String.format("%06d", random.nextInt(999999)));
        activation.setRegistrationDate(new Date(System.currentTimeMillis()));
        activation.setSchool(school);
        activation.setUser(user);
        iActivationRepository.save(activation);
    }
    

    public List<User> getAll() {
        List<User> actualList = new ArrayList<User>();
        this.userRepository.findAll().iterator().forEachRemaining(actualList::add);
        return actualList;
    }

    public void activate(ActivationRequest activation) {
        Activation savedActivation = this.iActivationRepository.findByCode(activation.code()).orElseThrow();
        User user = this.userRepository.findByEmail(activation.userMail()).orElseThrow();
        if(savedActivation.getUser().getEmail().equals(user.getEmail())) {
            user.setActive(true);
            this.userRepository.save(user);
        } else {
            // throws an error with right message
        }
    }


    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.userRepository
            .findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Pas d'utilisateur pour cet identifiant"));
    }
}

package synapseforge.crud.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedTestUsers();
    }

    private void seedTestUsers() {
        List<User> testUsers = Arrays.asList(
            createTestUser("Alice Silva", "alice.silva@teste.com"),
            createTestUser("Antonio Santos", "antonio.santos@teste.com")
        );

        for (User user : testUsers) {
            if (!userRepository.findByEmail(user.getEmail()).isPresent()) {
                userRepository.save(user);
                System.out.println("Usuário de teste criado: " + user.getNome());
            } else {
                System.out.println("Usuário de teste já existe: " + user.getNome());
            }
        }
    }

    private User createTestUser(String nome, String email) {
        User user = new User();
        user.setNome(nome);
        user.setEmail(email);
        user.setSenha(passwordEncoder.encode("1234"));
        user.setRole(Role.CLIENTE);
        user.setAtivo(true);
        user.setCriadoEm(LocalDateTime.now());
        return user;
    }
}

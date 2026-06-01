package synapseforge.crud.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import synapseforge.crud.infrastructure.entity.Acabamento;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CorRepository corRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        migrarUsuariosExistentes();
        seedTestUsers();
        seedTestCores();
    }

    // Sets emailConfirmado=true for users created before the email confirmation feature
    private void migrarUsuariosExistentes() {
        List<User> naoConfirmados = userRepository.findByEmailConfirmadoFalseAndEmailConfirmTokenIsNull();
        for (User user : naoConfirmados) {
            user.setEmailConfirmado(true);
            userRepository.save(user);
            System.out.println("Migrado (emailConfirmado=true): " + user.getEmail());
        }
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
        user.setEmailConfirmado(true);
        user.setCriadoEm(LocalDateTime.now());
        return user;
    }

    private void seedTestCores() {
        List<String> emails = Arrays.asList("alice.silva@teste.com", "antonio.santos@teste.com", "victoreidtrl@gmail.com");
        for (String email : emails) {
            userRepository.findByEmail(email)
                    .ifPresent(user -> seedCoresParaUsuario(user.getId(), user.getNome()));
        }
    }

    private void seedCoresParaUsuario(String usuarioId, String nomeUsuario) {
        Set<String> existentes = corRepository.findByUsuarioId(usuarioId).stream()
                .map(Cor::getNome)
                .collect(Collectors.toSet());

        List<Cor> paleta = Arrays.asList(
                novaCor(usuarioId, "Vermelho Queimado", "Coral Tintas", "CT-204", "#963A28", Acabamento.FOSCO, 450, 500, 0.28),
                novaCor(usuarioId, "Azul Cobalto", "Sherwin-Williams", "SW-118", "#1E40AF", Acabamento.BRILHANTE, 1250, 500, 0.36),
                novaCor(usuarioId, "Verde Oliva", "Coral Tintas", "CT-331", "#5E6B33", Acabamento.CETIM, 300, 500, 0.30),
                novaCor(usuarioId, "Bege Areia", "Suvinil", "SV-072", "#CDBA98", Acabamento.FOSCO, 980, 500, 0.25),
                novaCor(usuarioId, "Preto Fosco", "Weg Tintas", "WG-001", "#1E1E1E", Acabamento.FOSCO, 620, 500, 0.31),
                novaCor(usuarioId, "Branco Gelo", "Suvinil", "SV-010", "#F1F0EA", Acabamento.CETIM, 1800, 500, 0.22),
                novaCor(usuarioId, "Terracota", "Coral Tintas", "CT-289", "#C26A45", Acabamento.FOSCO, 210, 500, 0.29),
                novaCor(usuarioId, "Amarelo Mostarda", "Sherwin-Williams", "SW-447", "#D9A227", Acabamento.METALICO, 560, 500, 0.27)
        );

        int criadas = 0;
        for (Cor cor : paleta) {
            if (!existentes.contains(cor.getNome())) {
                corRepository.save(cor);
                criadas++;
            }
        }

        if (criadas > 0) {
            System.out.println("Cores de teste criadas para " + nomeUsuario + ": " + criadas);
        } else {
            System.out.println("Cores de teste já existem para " + nomeUsuario);
        }
    }

    private Cor novaCor(String usuarioId, String nome, String fornecedor, String codigo, String hex,
                        Acabamento acabamento, int estoqueMl, int estoqueMinimoMl, double custoMl) {
        Cor cor = new Cor();
        cor.setUsuarioId(usuarioId);
        cor.setNome(nome);
        cor.setFornecedor(fornecedor);
        cor.setCodigo(codigo);
        cor.setHex(hex);
        cor.setAcabamento(acabamento);
        cor.setEstoqueMl(estoqueMl);
        cor.setEstoqueMinimoMl(estoqueMinimoMl);
        cor.setCustoMl(custoMl);
        cor.setCriadoEm(LocalDateTime.now());
        cor.setAtualizadoEm(LocalDateTime.now());
        return cor;
    }
}

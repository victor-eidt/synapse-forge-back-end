package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder encoder;
    private final EmailService emailService;

    public User toEntity(UserRequestDTO dto) {
        User user = new User();

        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setSenha(dto.getSenha());
        user.setCpf(dto.getCpf());
        user.setTelefone(dto.getTelefone());
        user.setRole(dto.getRole());

        return user;
    }

    public UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getCpf(),
                user.getTelefone(),
                user.getRole() != null ? user.getRole().name() : null
        );
    }

    public User criar(User user) {
        if (repository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado");
        }

        user.setSenha(encoder.encode(user.getSenha()));
        user.setCriadoEm(LocalDateTime.now());
        user.setAtivo(true);

        return repository.save(user);
    }

    public List<User> listar() {
        return repository.findAll();
    }

    public Optional<User> buscarPorId(String id) {
        return repository.findById(id);
    }

    public User atualizar(String id, UserRequestDTO dto) {

        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setCpf(dto.getCpf());
        user.setTelefone(dto.getTelefone());

        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            user.setSenha(encoder.encode(dto.getSenha()));
        }

        return repository.save(user);
    }

    public void deletar(String id) {
        repository.deleteById(id);
    }

    public List<User> criarVarios(List<User> users) {

        users.forEach(user -> {
            user.setSenha(encoder.encode(user.getSenha()));
            user.setCriadoEm(LocalDateTime.now());
            user.setAtivo(true);
        });

        return repository.saveAll(users);
    }

    public List<User> buscarPorNome(String nome) {
        if (nome == null || nome.trim().length() < 3) {
            throw new RuntimeException("Mínimo 3 caracteres para busca");
        }
        return repository.findByNomeIgnoreCaseContaining(nome.trim());
    }

    public Map<String, String> solicitarMudancaEmail(String id, String novoEmail) {

        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (repository.findByEmail(novoEmail).isPresent()) {
            throw new RuntimeException("Este email já está em uso");
        }

        String token = UUID.randomUUID().toString();
        user.setEmailPendente(novoEmail);
        user.setEmailMudancaToken(token);
        user.setEmailMudancaTokenExpira(LocalDateTime.now().plusHours(1));
        repository.save(user);

        emailService.enviarConfirmacaoMudancaEmail(novoEmail, user.getNome(), token);

        return Map.of("mensagem", "Email de confirmação enviado para " + novoEmail);
    }

    public void confirmarMudancaEmail(String token) {

        User user = repository.findByEmailMudancaToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido ou já utilizado"));

        if (user.getEmailMudancaTokenExpira() == null ||
                user.getEmailMudancaTokenExpira().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        user.setEmail(user.getEmailPendente());
        user.setEmailPendente(null);
        user.setEmailMudancaToken(null);
        user.setEmailMudancaTokenExpira(null);
        repository.save(user);
    }

}
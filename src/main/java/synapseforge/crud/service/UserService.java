package synapseforge.crud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private BCryptPasswordEncoder encoder;

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
                user.getRole().name()
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

    public User atualizar(String id, User userAtualizado) {

        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setNome(userAtualizado.getNome());
        user.setEmail(userAtualizado.getEmail());
        user.setCpf(userAtualizado.getCpf());
        user.setTelefone(userAtualizado.getTelefone());
        user.setRole(userAtualizado.getRole());

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


}
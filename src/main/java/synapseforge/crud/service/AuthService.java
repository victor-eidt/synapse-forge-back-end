package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.User.LoginDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;
import synapseforge.crud.infrastructure.security.JwtService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder encoder;
    private final JwtService jwtService;

    // 🔐 CADASTRO
    public UserResponseDTO cadastro(UserRequestDTO dto) {

        // 1. Verifica se email já existe
        repository.findByEmail(dto.getEmail())
                .ifPresent(u -> {
                    throw new RuntimeException("Email já cadastrado");
                });

        // 2. Criptografa senha
        String senhaCriptografada = encoder.encode(dto.getSenha());

        // 3. Cria objeto User
        User user = new User();
        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setSenha(senhaCriptografada);
        user.setCpf(dto.getCpf());
        user.setTelefone(dto.getTelefone());
        user.setRole(dto.getRole());
        user.setAtivo(true);
        user.setCriadoEm(LocalDateTime.now());
        user.setTentativasLogin(0);

        // 4. Salva no banco
        repository.save(user);

        // 5. Retorna DTO (sem senha)
        return new UserResponseDTO(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    // 🔐 LOGIN
    public String login(LoginDTO dto) {

        // 1. Busca usuário por email
        User user = repository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Verifica se está bloqueado
        if (user.getBloqueadoEm() != null &&
                user.getBloqueadoEm().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Conta bloqueada temporariamente");
        }

        // 3. Verifica senha
        if (!encoder.matches(dto.getSenha(), user.getSenha())) {

            user.setTentativasLogin(user.getTentativasLogin() + 1);

            // 4. Bloqueia após 5 tentativas
            if (user.getTentativasLogin() >= 5) {
                user.setBloqueadoEm(LocalDateTime.now().plusMinutes(15));
            }

            repository.save(user);
            throw new RuntimeException("Senha incorreta");
        }

        // 5. Reset tentativas
        user.setTentativasLogin(0);
        user.setBloqueadoEm(null);
        repository.save(user);


        return jwtService.generateToken(user.getId());
    }
}
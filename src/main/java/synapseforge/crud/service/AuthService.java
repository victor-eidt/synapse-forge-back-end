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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder encoder;
    private final JwtService jwtService;

    // CADASTRO
    public UserResponseDTO cadastro(UserRequestDTO dto) {

        repository.findByEmail(dto.getEmail())
                .ifPresent(u -> {
                    throw new RuntimeException("Email já cadastrado");
                });

        String senhaCriptografada = encoder.encode(dto.getSenha());

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

        repository.save(user);

        return new UserResponseDTO(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    // LOGIN
    public String login(LoginDTO dto) {

        User user = repository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getBloqueadoEm() != null &&
                user.getBloqueadoEm().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Conta bloqueada temporariamente");
        }

        if (!encoder.matches(dto.getSenha(), user.getSenha())) {
            user.setTentativasLogin(user.getTentativasLogin() + 1);

            if (user.getTentativasLogin() >= 5) {
                user.setBloqueadoEm(LocalDateTime.now().plusMinutes(15));
            }

            repository.save(user);
            throw new RuntimeException("Senha incorreta");
        }

        user.setTentativasLogin(0);
        user.setBloqueadoEm(null);
        repository.save(user);

        return jwtService.generateToken(user.getId());
    }

    // ESQUECI SENHA
    public String esqueciSenha(String email) {

        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpira(LocalDateTime.now().plusHours(1));
        repository.save(user);

        // TODO: enviar token por e-mail
        return token;
    }

    // REDEFINIR SENHA
    public void redefinirSenha(String email, String token, String novaSenha) {

        User user = repository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (!user.getEmail().equals(email)) {
            throw new RuntimeException("Token inválido");
        }

        if (user.getResetTokenExpira() == null ||
                user.getResetTokenExpira().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        user.setSenha(encoder.encode(novaSenha));
        user.setResetToken(null);
        user.setResetTokenExpira(null);
        repository.save(user);
    }
}

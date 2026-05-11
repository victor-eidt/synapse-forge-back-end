package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.User.LoginDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;
import synapseforge.crud.infrastructure.security.JwtService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder encoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public Map<String, String> cadastro(UserRequestDTO dto) {

        repository.findByEmail(dto.getEmail())
                .ifPresent(u -> {
                    throw new RuntimeException("Email já cadastrado");
                });

        String confirmToken = UUID.randomUUID().toString();

        User user = new User();
        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setSenha(encoder.encode(dto.getSenha()));
        user.setCpf(dto.getCpf());
        user.setTelefone(dto.getTelefone());
        user.setRole(dto.getRole());
        user.setAtivo(true);
        user.setCriadoEm(LocalDateTime.now());
        user.setTentativasLogin(0);
        user.setEmailConfirmado(false);
        user.setEmailConfirmToken(confirmToken);
        user.setEmailConfirmTokenExpira(LocalDateTime.now().plusHours(24));

        repository.save(user);

        emailService.enviarConfirmacaoCadastro(user.getEmail(), user.getNome(), confirmToken);

        return Map.of("mensagem", "Conta criada! Verifique seu email para confirmar o acesso.");
    }

    public Map<String, String> login(LoginDTO dto) {

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

        if (!user.isEmailConfirmado()) {
            throw new RuntimeException("EMAIL_NAO_CONFIRMADO");
        }

        user.setTentativasLogin(0);
        user.setBloqueadoEm(null);
        repository.save(user);

        String token = jwtService.generateToken(user.getId());
        return Map.of(
            "access_token", token,
            "user_id", user.getId()
        );
    }

    public Map<String, String> confirmarEmail(String token) {

        User user = repository.findByEmailConfirmToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido ou já utilizado"));

        if (user.getEmailConfirmTokenExpira() == null ||
                user.getEmailConfirmTokenExpira().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        user.setEmailConfirmado(true);
        user.setEmailConfirmToken(null);
        user.setEmailConfirmTokenExpira(null);
        user.setTentativasLogin(0);
        repository.save(user);

        String jwt = jwtService.generateToken(user.getId());
        return Map.of(
            "access_token", jwt,
            "user_id", user.getId()
        );
    }

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

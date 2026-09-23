package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.User.LoginDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.Role;
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
    private final EquipeService equipeService;

    // =========================================================
    // CADASTRO DE CLIENTE
    // =========================================================

    public Map<String, String> cadastro(UserRequestDTO dto) {

        return cadastrarUsuario(dto, Role.CLIENTE);
    }

    // =========================================================
    // CADASTRO DE GERENTE
    // =========================================================

    public Map<String, String> cadastroGerente(UserRequestDTO dto) {

        return cadastrarUsuario(dto, Role.GERENTE);
    }

    // =========================================================
    // MÉTODO INTERNO DE CADASTRO
    // =========================================================

    private Map<String, String> cadastrarUsuario(
            UserRequestDTO dto,
            Role role
    ) {

        repository.findByEmail(dto.getEmail())
                .ifPresent(u -> {
                    throw new RuntimeException("Email já cadastrado");
                });

        /*
         * Gerente nasce com a loja (equipe): no SaaS todo dado pertence a
         * uma equipe, então não existe gerente sem equipe.
         */
        String nomeEquipe =
                dto.getNomeEquipe() == null
                        ? ""
                        : dto.getNomeEquipe().trim();

        if (role == Role.GERENTE && nomeEquipe.isEmpty()) {
            throw new RuntimeException("Informe o nome da loja");
        }

        String confirmToken = UUID.randomUUID().toString();

        User user = new User();

        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setSenha(encoder.encode(dto.getSenha()));
        user.setCpf(dto.getCpf());
        user.setTelefone(dto.getTelefone());

        /*
         * O papel do usuário é definido pelo endpoint utilizado.
         *
         * /auth/cadastro
         * -> CLIENTE
         *
         * /auth/cadastro-gerente
         * -> GERENTE
         *
         * Não usamos dto.getRole() para evitar que o frontend
         * consiga escolher livremente uma role privilegiada.
         */
        user.setRole(role);

        user.setAtivo(true);
        user.setCriadoEm(LocalDateTime.now());
        user.setTentativasLogin(0);
        user.setEmailConfirmado(false);
        user.setEmailConfirmToken(confirmToken);
        user.setEmailConfirmTokenExpira(
                LocalDateTime.now().plusHours(24)
        );

        repository.save(user);

        Equipe equipe = null;

        if (role == Role.GERENTE) {

            equipe = equipeService.criar(
                    user.getId(),
                    nomeEquipe,
                    null,
                    null
            );

            user.setEquipeId(equipe.getId());
            repository.save(user);
        }

        try {

            emailService.enviarConfirmacaoCadastro(
                    user.getEmail(),
                    user.getNome(),
                    confirmToken
            );

        } catch (RuntimeException e) {

            if (equipe != null) {
                equipeService.deletar(equipe.getId(), user.getId());
            }

            repository.delete(user);
            throw e;
        }

        return Map.of(
                "mensagem",
                "Conta criada! Verifique seu email para confirmar o acesso."
        );
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public Map<String, String> login(LoginDTO dto) {

        User user = repository.findByEmail(dto.getEmail())
                .orElseThrow(
                        () -> new RuntimeException(
                                "Usuário não encontrado"
                        )
                );

        if (
                user.getBloqueadoEm() != null
                        && user.getBloqueadoEm()
                        .isAfter(LocalDateTime.now())
        ) {

            throw new RuntimeException(
                    "CONTA_BLOQUEADA:"
                            + minutosRestantes(
                            user.getBloqueadoEm()
                    )
            );
        }

        if (!encoder.matches(dto.getSenha(), user.getSenha())) {

            user.setTentativasLogin(
                    user.getTentativasLogin() + 1
            );

            if (user.getTentativasLogin() >= 5) {

                user.setBloqueadoEm(
                        LocalDateTime.now().plusMinutes(15)
                );

                repository.save(user);

                throw new RuntimeException(
                        "CONTA_BLOQUEADA:15"
                );
            }

            repository.save(user);

            throw new RuntimeException(
                    "Senha incorreta"
            );
        }

        if (!user.isEmailConfirmado()) {

            throw new RuntimeException(
                    "EMAIL_NAO_CONFIRMADO"
            );
        }

        user.setTentativasLogin(0);
        user.setBloqueadoEm(null);

        repository.save(user);

        String token = jwtService.generateToken(
                user.getId(),
                user.getRole()
        );

        return Map.of(
                "access_token", token,
                "user_id", user.getId()
        );
    }

    // =========================================================
    // CALCULAR MINUTOS RESTANTES DO BLOQUEIO
    // =========================================================

    private long minutosRestantes(
            LocalDateTime bloqueadoEm
    ) {

        long minutos =
                java.time.Duration
                        .between(
                                LocalDateTime.now(),
                                bloqueadoEm
                        )
                        .toMinutes()
                        + 1;

        return Math.max(minutos, 1);
    }

    // =========================================================
    // CONFIRMAR EMAIL
    // =========================================================

    public Map<String, String> confirmarEmail(
            String token
    ) {

        User user = repository.findByEmailConfirmToken(token)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Token inválido ou já utilizado"
                        )
                );

        if (
                user.getEmailConfirmTokenExpira() == null
                        || user.getEmailConfirmTokenExpira()
                        .isBefore(LocalDateTime.now())
        ) {

            throw new RuntimeException(
                    "Token expirado"
            );
        }

        user.setEmailConfirmado(true);
        user.setEmailConfirmToken(null);
        user.setEmailConfirmTokenExpira(null);
        user.setTentativasLogin(0);

        repository.save(user);

        String jwt = jwtService.generateToken(
                user.getId(),
                user.getRole()
        );

        return Map.of(
                "access_token", jwt,
                "user_id", user.getId()
        );
    }

    // =========================================================
    // ESQUECI A SENHA
    // =========================================================

    public void esqueciSenha(
            String email
    ) {

        // Não revela se o email existe:
        // apenas envia o link quando houver conta.
        repository.findByEmail(email)
                .ifPresent(user -> {

                    String token =
                            UUID.randomUUID().toString();

                    user.setResetToken(token);

                    user.setResetTokenExpira(
                            LocalDateTime.now().plusHours(1)
                    );

                    repository.save(user);

                    emailService.enviarRecuperacaoSenha(
                            user.getEmail(),
                            user.getNome(),
                            token
                    );
                });
    }

    // =========================================================
    // REDEFINIR SENHA
    // =========================================================

    public void redefinirSenha(
            String email,
            String token,
            String novaSenha
    ) {

        User user = repository.findByResetToken(token)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Token inválido"
                        )
                );

        if (!user.getEmail().equals(email)) {

            throw new RuntimeException(
                    "Token inválido"
            );
        }

        if (
                user.getResetTokenExpira() == null
                        || user.getResetTokenExpira()
                        .isBefore(LocalDateTime.now())
        ) {

            throw new RuntimeException(
                    "Token expirado"
            );
        }

        user.setSenha(
                encoder.encode(novaSenha)
        );

        user.setResetToken(null);
        user.setResetTokenExpira(null);
        user.setTentativasLogin(0);
        user.setBloqueadoEm(null);

        repository.save(user);
    }
}